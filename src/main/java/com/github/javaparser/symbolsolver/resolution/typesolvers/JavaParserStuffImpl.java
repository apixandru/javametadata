package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.apixandru.java.visitors.JpUtils.findFilePath;

public class JavaParserStuffImpl implements JavaParserStuff {

    private final Map<String, CompilationUnit> compilationUnits = new HashMap<>();

    private final Map<String, CompilationUnit> compilationUnitsForTypes = new HashMap<>();

    public JavaParserStuffImpl(List<CompilationUnit> compilationUnits) {
        for (CompilationUnit compilationUnit : compilationUnits) {
            String filePath = findFilePath(compilationUnit);
            this.compilationUnits.put(filePath, compilationUnit);

            for (TypeDeclaration<?> type : compilationUnit.getTypes()) {
                String fullyQualifiedName = type.getFullyQualifiedName()
                        .orElseThrow(() -> new IllegalArgumentException("Missing fully qualified name!"));

                CompilationUnit put = compilationUnitsForTypes.put(fullyQualifiedName, compilationUnit);
                ensureNotAlreadyMapped(filePath, fullyQualifiedName, put);
            }
        }
    }

    private void ensureNotAlreadyMapped(String currentPath, String fullyQualifiedName, CompilationUnit put) {
        if (put != null) {
            throw new IllegalStateException(fullyQualifiedName + " mapped by " + currentPath + " was already mapped in " + findFilePath(put));
        }
    }

    @Override
    public Optional<CompilationUnit> findCompilationUnit(Path srcFile) {
        CompilationUnit compilationUnit = compilationUnits.get(srcFile.toAbsolutePath().toFile().getAbsolutePath());
        return Optional.ofNullable(compilationUnit);
    }

    @Override
    public Optional<CompilationUnit> findCompilationUnitByTypeName(String typeName) {
        CompilationUnit compilationUnit = compilationUnitsForTypes.get(typeName);
        return Optional.ofNullable(compilationUnit);

    }

}
