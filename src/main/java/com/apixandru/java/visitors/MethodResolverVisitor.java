package com.apixandru.java.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MethodResolverVisitor extends VoidVisitorAdapter<MethodCsvReport> {

    public static final MethodResolverVisitor INSTANCE = new MethodResolverVisitor();

    @Override
    public void visit(MethodCallExpr n, MethodCsvReport arg) {
        super.visit(n, arg);
        Optional<Expression> scope = n.getScope();

        try {
            ResolvedMethodDeclaration resolve = n.resolve();
            handle(n, arg, resolve);
        } catch (Exception ex) {
            arg.writeError(n, "Cannot resolve " + n, getMessage(ex));
        }

        if (scope.isPresent()) {
            Expression expression = scope.get();
            try {
                ResolvedReferenceTypeDeclaration typeDeclaration = getTypeDeclaration(expression);
                arg.add(expression, typeDeclaration);
            } catch (Exception ex) {
                arg.writeError(expression, "Cannot resolve " + expression, getMessage(ex));
            }
        }
    }

    private ResolvedReferenceType getResolvedType(Expression expression) {
        if (expression.isClassExpr()) {
            return ((ClassExpr) expression)
                    .getType()
                    .resolve()
                    .asReferenceType();
        }
        return expression.calculateResolvedType()
                .asReferenceType();
    }

    private ResolvedReferenceTypeDeclaration getTypeDeclaration(Expression expression) {
        if (expression.isThisExpr()) {
            ThisExpr thisExpr = expression.asThisExpr();
            ResolvedTypeDeclaration resolve = thisExpr.resolve();
            return resolve.asReferenceType();
        }

        return getResolvedType(expression)
                .getTypeDeclaration()
                .orElseThrow(() -> new IllegalArgumentException("Missing type declaration for " + expression));
    }

    private void handle(MethodCallExpr n, MethodCsvReport arg, ResolvedMethodDeclaration resolve) {
        if (resolve instanceof ReflectionMethodDeclaration) {
            ReflectionMethodDeclaration rmd = (ReflectionMethodDeclaration) resolve;
            handle(n, arg, rmd);
        }
        JavaParserMethodDeclaration jpmd = (JavaParserMethodDeclaration) resolve;
        MethodDeclaration wrappedNode = jpmd.getWrappedNode();
        handle(n, arg, wrappedNode);
    }

    private void handle(MethodCallExpr n, MethodCsvReport arg, MethodDeclaration wrappedNode) {
        Node parentNode = wrappedNode.getParentNode()
                .orElseThrow(() -> new IllegalArgumentException("No parent found!"));
        if (parentNode instanceof ClassOrInterfaceDeclaration || parentNode instanceof EnumDeclaration) {
            TypeDeclaration<?> node = (TypeDeclaration<?>) parentNode;
            arg.add(n.getName(), node, wrappedNode);
        } else {
            throw new IllegalStateException("");
        }
    }

    private void handle(MethodCallExpr n, MethodCsvReport arg, ReflectionMethodDeclaration rmd) {
        SimpleName actualMethodName = n.getName();
        List<String> args = new ArrayList<>();
        for (int i = 0; i < rmd.getNumberOfParams(); i++) {
            ResolvedParameterDeclaration param = rmd.getParam(i);
            args.add(param.getType().asReferenceType().getQualifiedName());
        }
        arg.add(actualMethodName, actualMethodName, args, rmd.declaringType().asReferenceType().getQualifiedName());
    }

    private String getMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null) {
            message = ex.getClass() + " had no message";
        }
        return message;
    }

}
