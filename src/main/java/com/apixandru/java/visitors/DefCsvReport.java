package com.apixandru.java.visitors;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.util.List;

public class DefCsvReport extends AbstractCsvReport {

    public DefCsvReport(File fakeOutput) {
        super(fakeOutput, "def");
    }

    public void add(ClassOrInterfaceDeclaration owner) {
        try {
            writeRegular(owner, owner.getFullyQualifiedName().get());
        } catch (Exception ex) {
            writeError(owner, owner.getNameAsString(), ex.getMessage());
        }
    }

    @Override
    protected List<Object> getWriterHeader() {
        return List.of("line", "col", "type");
    }

}
