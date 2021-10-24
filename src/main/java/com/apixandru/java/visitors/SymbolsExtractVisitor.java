package com.apixandru.java.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.Optional;

public class SymbolsExtractVisitor extends VoidVisitorAdapter<TypesCsvReport> {

    public static final SymbolsExtractVisitor INSTANCE = new SymbolsExtractVisitor();

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

    private void tryResolveAgain(ClassOrInterfaceType n, TypesCsvReport arg, UnsolvedSymbolException ex) {
        ClassOrInterfaceType topLevelClass = getTopLevelClass(n);
        if (topLevelClass != null) {
            arg.writePackage(n, n, topLevelClass);
            return;
        }
        arg.writeError(n, "Bad symbol " + n, ex.getMessage());
        System.out.println("Bad symbol " + n);
    }

    @Override
    public void visit(TypeParameter n, TypesCsvReport arg) {
        super.visit(n, arg);
        arg.writeRegular(n, n);
    }

    @Override
    public void visit(ClassOrInterfaceType n, TypesCsvReport arg) {
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
    public void visit(VariableDeclarator n, TypesCsvReport arg) {
        super.visit(n, arg);
        if (n.getType().isPrimitiveType()) {
            return;
        }
        try {
            ResolvedType resolve = n.getType().resolve();
            System.out.println(resolve);
//            arg.writeRegular(n, n.resolve().getQualifiedName());
        } catch (UnsolvedSymbolException ex) {
            arg.writeError(n, "Unresolved symbol " + n, ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            arg.writeError(n, "Unresolvable symbol " + n, ex.getMessage());
        }


    }

    @Override
    public void visit(UnparsableStmt n, TypesCsvReport arg) {
        throw new IllegalStateException("Cannot parse " + n);
    }

}
