package com.apixandru.java.main;

import com.apixandru.java.visitors.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserStuffImpl;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ParsedTypeSolver;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.apixandru.java.visitors.JpUtils.findPath;

public class MainDataCollectorMultiThreadded {

    private static final Logger log = LoggerFactory.getLogger(MainDataCollectorMultiThreadded.class);

    public static void main2(String[] args) throws ExecutionException, InterruptedException {
        String analysisRoot = "C:/_/workdir/analysis/";

        long start = System.currentTimeMillis();
        Library library = Libraries.getHotspotLibrary();
        String targetDir = library.getOutputDir(analysisRoot);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<File> srcDirs = library.getSrcDirs();

        LinkedList<File> filesToVisit = new LinkedList<>(srcDirs);
        List<Future<CompilationUnit>> futureCompilationUnits = new LinkedList<>();
        List<CompilationUnit> compilationUnits = new LinkedList<>();

        int parsed = 0;
        while (!filesToVisit.isEmpty()) {
            File inputFile = filesToVisit.removeFirst();
            if (inputFile.isDirectory()) {
                // noinspection ConstantConditions
                filesToVisit.addAll(Arrays.asList(inputFile.listFiles()));
            } else if (!inputFile.getName().endsWith(".java")) {
                log.info("Skipping " + inputFile);
            } else {
                parsed++;
                log.info(parsed + "/" + filesToVisit.size() + " Parsing " + inputFile);
                futureCompilationUnits.add(executorService.submit(new CompilationUnitCallable(inputFile)));
            }
        }

        waitToFinish(futureCompilationUnits, compilationUnits);

        JavaParserStuffImpl jps = new JavaParserStuffImpl(compilationUnits);
        JavaSymbolSolver dsr = new JavaSymbolSolver(new ParsedTypeSolver(srcDirs, jps));
        compilationUnits.forEach(dsr::inject);

        AtomicInteger counter = new AtomicInteger();
        int total = compilationUnits.size();

        List<Future<Object>> stillProcessing = new ArrayList<>();
        for (CompilationUnit compilationUnit : compilationUnits) {
            @SuppressWarnings("rawtypes")
            Future submit = executorService.submit(() -> extracted(library, targetDir, counter, total, compilationUnit));
            stillProcessing.add(submit);
        }

        waitToFinish(stillProcessing, new ArrayList<>());

        executorService.shutdown();

        log.info("Took {}", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
    }

    private static void extracted(Library library, String targetDir, AtomicInteger counter, int total, CompilationUnit compilationUnit) {
        Path path = findPath(compilationUnit);
        File inputFile = path.toFile();
        log.info("[{}/{}] Visiting file {}", counter.incrementAndGet(), total, path);
        File outputFile = library.getOutputFile(inputFile, targetDir);
        generateReports(compilationUnit, outputFile);
    }

    private static void generateReports(CompilationUnit compilationUnit, File outputFile) {
//        try (CsvReports arg = new CsvReports(outputFile)) {
//            compilationUnit.accept(CsvReportVisitor.INSTANCE, arg);
//        }
        try (MethodCsvReport methodCsvReport = new MethodCsvReport(outputFile)) {
            compilationUnit.accept(MethodResolverVisitor.INSTANCE, methodCsvReport);
        }
        try (TypesCsvReport arg = new TypesCsvReport(outputFile)) {
            compilationUnit.accept(SymbolsExtractVisitor.INSTANCE, arg);
        }
        try (DefCsvReport arg = new DefCsvReport(outputFile)) {
            compilationUnit.accept(DefinitionExtractVisitor.INSTANCE, arg);
        }
    }

    public static JavaParser createParser() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setDetectOriginalLineSeparator(false)
                .setAttributeComments(false)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);

        return new JavaParser(configuration);
    }

    private static <T> void waitToFinish(List<Future<T>> futureUnits, List<T> units) throws ExecutionException, InterruptedException {
        int processed = 0;
        while (!futureUnits.isEmpty()) {
            sleepInterruptable(1000);
            Iterator<Future<T>> fit = futureUnits.iterator();
            while (fit.hasNext()) {
                Future<T> next = fit.next();
                if (next.isDone()) {
                    units.add(next.get());
                    fit.remove();
                    processed++;
                }
            }
            log.info("Processed {}/{}", futureUnits.size(), processed);
        }
    }

    private static void sleepInterruptable(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
