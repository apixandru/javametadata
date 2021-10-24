package com.apixandru.java.main;

import com.apixandru.java.visitors.JpUtils;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.util.concurrent.Callable;

public class CompilationUnitCallable implements Callable<CompilationUnit> {

    private static final ThreadLocal<JavaParser> javaParser = ThreadLocal.withInitial(MainDataCollectorMultiThreadded::createParser);

    private final File inputFile;

    public CompilationUnitCallable(File inputFile) {
        this.inputFile = inputFile;
    }

    @Override
    public CompilationUnit call() {
        JavaParser parser = javaParser.get();
        return JpUtils.parse(parser, inputFile);
    }

}
