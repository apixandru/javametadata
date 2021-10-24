package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.ast.CompilationUnit;

import java.nio.file.Path;
import java.util.Optional;

public interface JavaParserStuff {

    Optional<CompilationUnit> findCompilationUnit(Path srcFile);

    Optional<CompilationUnit> findCompilationUnitByTypeName(String typeName);

}
