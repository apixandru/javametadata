package com.apixandru.java.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class DefinitionExtractVisitor extends VoidVisitorAdapter<DefCsvReport> {

    public static final DefinitionExtractVisitor INSTANCE = new DefinitionExtractVisitor();

    @Override
    public void visit(ClassOrInterfaceDeclaration n, DefCsvReport arg) {
        super.visit(n, arg);
        arg.add(n);
    }

}
