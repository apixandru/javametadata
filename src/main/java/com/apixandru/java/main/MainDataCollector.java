package com.apixandru.java.main;

import com.apixandru.java.visitors.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MainDataCollector {

    public static void main(String[] args) throws FileNotFoundException {
        long start = System.currentTimeMillis();
        String analysisRoot = "C:/_/workdir/analysis/";

//        Library library = Libraries.getOpenJdkLibrary();
        Library library = Libraries.getHotspotLibrary();
//        Library library = Libraries.getCommonsLangLibrary();

        String targetDir = library.getOutputDir(analysisRoot);
        DelegatingSymbolResolver dsr = new DelegatingSymbolResolver();

        ParserConfiguration parserConfiguration = new ParserConfiguration()
                .setAttributeComments(false)
                .setDetectOriginalLineSeparator(false)
                .setSymbolResolver(dsr)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);

        JavaParser javaParser = new JavaParser(parserConfiguration);

        List<TypeSolver> parsers = library.getSrcDirs()
                .stream()
                .map(srcDir -> new JavaParserTypeSolver(srcDir, parserConfiguration))
                .collect(toList());

        dsr.setSr(new JavaSymbolSolver(new CombinedTypeSolver(parsers)));

//        LinkedList<File> filesToVisit = new LinkedList<>(Arrays.asList(new File("C:\\_\\workdir\\sources\\JDK\\openjdk8\\jdk8-b120\\jaxws\\com\\sun\\xml\\internal\\rngom\\parse\\compact\\CompactSyntax.java")));
        LinkedList<File> filesToVisit = new LinkedList<>(library.getSrcDirs());
        int parsed = 0;
        while (!filesToVisit.isEmpty()) {
            File inputFile = filesToVisit.removeFirst();
            if (inputFile.isDirectory()) {
                // noinspection ConstantConditions
                filesToVisit.addAll(Arrays.asList(inputFile.listFiles()));
            } else if (!inputFile.getName().endsWith(".java")) {
                System.out.println("Skipping " + inputFile);
            } else {
                parsed++;
                System.out.println(LocalTime.now() + " " + parsed + "/" + filesToVisit.size() + " Parsing " + inputFile);
                File outputFile = library.getOutputFile(inputFile, targetDir);
                CompilationUnit parse = JpUtils.parse(javaParser, inputFile);

//                try (CsvReports arg = new CsvReports(outputFile)) {
//                    parse.accept(CsvReportVisitor.INSTANCE, arg);
//                }

                try (MethodCsvReport methodCsvReport = new MethodCsvReport(outputFile)) {
                    parse.accept(MethodResolverVisitor.INSTANCE, methodCsvReport);
                }
                try (TypesCsvReport arg = new TypesCsvReport(outputFile)) {
                    parse.accept(SymbolsExtractVisitor.INSTANCE, arg);
                }
                try (DefCsvReport arg = new DefCsvReport(outputFile)) {
                    parse.accept(DefinitionExtractVisitor.INSTANCE, arg);
                }

            }
        }
        System.out.println("Took " + DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
    }

}
