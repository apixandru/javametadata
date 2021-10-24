/*
 * Copyright (C) 2015-2016 Federico Tomassetti
 * Copyright (C) 2017-2020 The JavaParser Team.
 *
 * This file is part of JavaParser.
 *
 * JavaParser can be used either under the terms of
 * a) the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * b) the terms of the Apache License
 *
 * You should have received a copy of both licenses in LICENCE.LGPL and
 * LICENCE.APACHE. Please refer to those files for details.
 *
 * JavaParser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 */

package com.github.javaparser.symbolsolver.resolution.typesolvers;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.cache.Cache;
import com.github.javaparser.symbolsolver.cache.GuavaCache;
import com.github.javaparser.symbolsolver.javaparser.Navigator;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.utils.FileUtils;
import com.google.common.cache.CacheBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class ParsedTypeSolver implements TypeSolver {

    private static final int CACHE_SIZE_UNSET = -1;

    private final JavaParserFacade javaParserFacade = JavaParserFacade.get(this);

    private final List<Path> srcDirs;
    private final JavaParserStuff javaParser;
    private final Cache<Path, Optional<CompilationUnit>> parsedFiles;
    private final Cache<Path, List<CompilationUnit>> parsedDirectories;
    private final Cache<String, SymbolReference<ResolvedReferenceTypeDeclaration>> foundTypes;

    private TypeSolver parent;

    public ParsedTypeSolver(List<File> srcDir, JavaParserStuff parserConfiguration) {
        this(toPaths(srcDir), parserConfiguration, CACHE_SIZE_UNSET);
    }

    public ParsedTypeSolver(List<Path> srcDirs, JavaParserStuff jps, long cacheSizeLimit) {
        for (Path srcDir : srcDirs) {
            if (!Files.exists(srcDir) || !Files.isDirectory(srcDir)) {
                throw new IllegalStateException("SrcDir does not exist or is not a directory: " + srcDir);
            }
        }
        this.srcDirs = srcDirs;
        parsedFiles = buildCache(cacheSizeLimit);
        parsedDirectories = buildCache(cacheSizeLimit);
        foundTypes = buildCache(cacheSizeLimit);
        javaParser = jps;
    }

    private static List<Path> toPaths(List<File> srcDir) {
        return srcDir.stream()
                .map(File::toPath)
                .collect(toList());
    }

    private <TKey, TValue> Cache<TKey, TValue> buildCache(long cacheSizeLimit) {
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder().softValues();
        if (cacheSizeLimit != CACHE_SIZE_UNSET) {
            cacheBuilder.maximumSize(cacheSizeLimit);
        }
        return new GuavaCache<>(cacheBuilder.build());
    }

    @Override
    public TypeSolver getParent() {
        return parent;
    }

    @Override
    public void setParent(TypeSolver parent) {
        Objects.requireNonNull(parent);
        if (this.parent != null) {
            throw new IllegalStateException("This TypeSolver already has a parent.");
        }
        if (parent == this) {
            throw new IllegalStateException("The parent of this TypeSolver cannot be itself.");
        }
        this.parent = parent;
    }

    private Optional<CompilationUnit> parse(Path srcFile) {
        try {
            Optional<Optional<CompilationUnit>> cachedParsedFile = parsedFiles.get(srcFile.toAbsolutePath());
            // If the value is already cached
            if (cachedParsedFile.isPresent()) {
                return cachedParsedFile.get();
            }

            // Otherwise load it
            if (!Files.exists(srcFile) || !Files.isRegularFile(srcFile)) {
                parsedFiles.put(srcFile.toAbsolutePath(), Optional.empty());
                return Optional.empty();
            }

            return getCompilationUnit(srcFile);
        } catch (IOException e) {
            throw new RuntimeException("Issue while parsing while type solving: " + srcFile.toAbsolutePath(), e);
        }
    }

    private Optional<CompilationUnit> getCompilationUnit(Path srcFile) throws IOException {
        Optional<CompilationUnit> compilationUnit = javaParser.findCompilationUnit(srcFile);
        parsedFiles.put(srcFile.toAbsolutePath(), compilationUnit);
        return compilationUnit;
    }

    /**
     * Note that this parse only files directly contained in this directory.
     * It does not traverse recursively all children directory.
     */
    private List<CompilationUnit> parseDirectory(Path srcDirectory) {
        return parseDirectory(srcDirectory, false);
    }

    private List<CompilationUnit> parseDirectoryRecursively(Path srcDirectory) {
        return parseDirectory(srcDirectory, true);
    }

    private List<CompilationUnit> parseDirectory(Path srcDirectory, boolean recursively) {
        try {
            Optional<List<CompilationUnit>> cachedValue = parsedDirectories.get(srcDirectory.toAbsolutePath());
            if (cachedValue.isPresent()) {
                return cachedValue.get();
            }

            // If not cached, we need to load it
            List<CompilationUnit> units = new ArrayList<>();
            if (Files.exists(srcDirectory)) {
                try (DirectoryStream<Path> srcDirectoryStream = Files.newDirectoryStream(srcDirectory)) {
                    srcDirectoryStream.forEach(file -> parse(recursively, units, file));
                }
            }
            parsedDirectories.put(srcDirectory.toAbsolutePath(), units);
            return units;
        } catch (IOException e) {
            throw new RuntimeException("Unable to parse directory due to an exception. Directory:" + srcDirectory.toAbsolutePath(), e);
        }

    }

    private void parse(boolean recursively, List<CompilationUnit> units, Path file) {
        if (file.getFileName().toString().toLowerCase().endsWith(".java")) {
            parse(file).ifPresent(units::add);
        } else if (recursively && file.toFile().isDirectory()) {
            units.addAll(parseDirectoryRecursively(file));
        }
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
        return foundTypes.get(name)
                .orElseGet(() -> trySolveAndCache(name));
    }

    private SymbolReference<ResolvedReferenceTypeDeclaration> trySolveAndCache(String name) {
        SymbolReference<ResolvedReferenceTypeDeclaration> result = tryToSolveTypeUncached(name);
        foundTypes.put(name, result);
        return result;
    }

    private SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveTypeUncached(String name) {
        String[] nameElements = name.split("\\.");

        for (Path srcDir : srcDirs) {
            SymbolReference<ResolvedReferenceTypeDeclaration> type = resolving(nameElements, srcDir);
            if (type != null) {
                return type;
            }
        }

        return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
    }

    private SymbolReference<ResolvedReferenceTypeDeclaration> resolving(String[] nameElements, Path srcDir) {
        for (int i = nameElements.length; i > 0; i--) {
            StringBuilder filePath = new StringBuilder(srcDir.toAbsolutePath().toString());
            for (int j = 0; j < i; j++) {
                filePath.append(File.separator)
                        .append(nameElements[j]);
            }
            filePath.append(".java");

            StringBuilder typeName = new StringBuilder();
            for (int j = i - 1; j < nameElements.length; j++) {
                if (j != i - 1) {
                    typeName.append(".");
                }
                typeName.append(nameElements[j]);
            }

            String dirToParse = null;
            // As an optimization we first try to look in the canonical position where we expect to find the file
            if (FileUtils.isValidPath(filePath.toString())) {
                Path srcFile = Paths.get(filePath.toString());
                Optional<CompilationUnit> optionalCompilationUnit = parse(srcFile);
                if (optionalCompilationUnit.isPresent()) {
                    CompilationUnit compilationUnit = optionalCompilationUnit.get();
                    SymbolReference<ResolvedReferenceTypeDeclaration> type = findType(compilationUnit, typeName.toString());
                    if (type != null) {
                        return type;
                    }
                }
                dirToParse = srcFile.getParent().normalize().toString();
            } else {
                dirToParse = FileUtils.getParentPath(filePath.toString());
            }

            // If this is not possible we parse all files
            // We try just in the same package, for classes defined in a file not named as the class itself
            if (FileUtils.isValidPath(dirToParse)) {
                List<CompilationUnit> compilationUnits = parseDirectory(Paths.get(dirToParse));
                for (CompilationUnit compilationUnit : compilationUnits) {
                    SymbolReference<ResolvedReferenceTypeDeclaration> type = findType(compilationUnit, typeName.toString());
                    if (type != null) {
                        return type;
                    }
                }
            }
        }
        return null;
    }

    private SymbolReference<ResolvedReferenceTypeDeclaration> findType(CompilationUnit compilationUnit, String typeName) {
        return Navigator.findType(compilationUnit, typeName)
                .map(javaParserFacade::getTypeDeclaration)
                .map(SymbolReference::solved)
                .orElse(null);

    }

}
