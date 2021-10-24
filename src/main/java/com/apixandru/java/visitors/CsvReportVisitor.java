package com.apixandru.java.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class CsvReportVisitor extends VoidVisitorAdapter<CsvReports> {

    public static final CsvReportVisitor INSTANCE = new CsvReportVisitor();

    private static final Logger log = LoggerFactory.getLogger(CsvReportVisitor.class);

    private static ClassOrInterfaceType getTopLevelClass(ClassOrInterfaceType coit) {
        Optional<Node> optionalParentNode = coit.getParentNode();
        if (optionalParentNode.isEmpty()) {
            return null;
        }
        Node node = optionalParentNode.get();
        if (node instanceof ClassOrInterfaceType) {
            coit = (ClassOrInterfaceType) node;
            try {
                coit.resolve();
                return coit; // it's resolvable, this is the real deal guys!
            } catch (Exception ex) {
//                ex.printStackTrace();
                return getTopLevelClass(coit);
            }
        }
        return null;
    }

    private void tryResolveAgain(ClassOrInterfaceType n, CsvReports arg, UnsolvedSymbolException ex) {
        ClassOrInterfaceType topLevelClass = getTopLevelClass(n);
        if (topLevelClass != null) {
            arg.writePackage(n, n, topLevelClass);
            return;
        }
        arg.writeError(n, "Bad symbol " + n, ex.getMessage());
        log.error("Bad symbol " + n);
    }

    @Override
    public void visit(TypeParameter n, CsvReports arg) {
        super.visit(n, arg);
        arg.writeRegular(n, n);
    }

    @Override
    public void visit(ClassOrInterfaceType n, CsvReports arg) {
        super.visit(n, arg);
        try {
            arg.writeRegular(n, n.resolve().getQualifiedName());
        } catch (UnsolvedSymbolException ex) {
            tryResolveAgain(n, arg, ex);
        } catch (UnsupportedOperationException ex) {
            arg.writeError(n, "Unresolvable symbol " + n, ex.getMessage());
        }
    }

    @Override
    public void visit(VariableDeclarator n, CsvReports arg) {
        super.visit(n, arg);
        Type type = n.getType();
        if (type.isPrimitiveType()) {
            return;
        }
        try {
            ResolvedType resolve = type.resolve();
            handleResolvedType(n, arg, resolve);
        } catch (UnsolvedSymbolException ex) {
            arg.writeError(n, "Unresolved symbol " + n, ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            arg.writeError(n, "Unresolvable symbol " + n, ex.getMessage());
        }
    }

    private void handleResolvedType(VariableDeclarator n, CsvReports arg, ResolvedType resolve) {
        if (resolve.isPrimitive()) {
            return;
        }
        if (resolve instanceof ReferenceTypeImpl) {
            ReferenceTypeImpl type = (ReferenceTypeImpl) resolve;
            arg.writeRegular(n, type.getQualifiedName());
        } else if (resolve instanceof ResolvedArrayType) {
            ResolvedArrayType resolvedArrayType = (ResolvedArrayType) resolve;
            handleResolvedType(n, arg, resolvedArrayType.getComponentType());
        } else if (resolve.isTypeVariable()) {
            arg.writeError(n, "Type variables not supported yet: " + n, n.toString());
            log.warn("Not supported yet! {}", resolve);
        } else {
            throw new IllegalStateException(resolve.getClass().getName());
        }
    }

    @Override
    public void visit(UnparsableStmt n, CsvReports arg) {
        throw new IllegalStateException("Cannot parse " + n);
    }

}
