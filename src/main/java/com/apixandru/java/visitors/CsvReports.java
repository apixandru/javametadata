package com.apixandru.java.visitors;

import com.github.javaparser.Position;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.opencsv.CSVWriter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.apixandru.java.visitors.JpUtils.getStart;
import static com.apixandru.util.CsvUtils.newCsvWriter;
import static com.apixandru.util.CsvUtils.write;

public class CsvReports implements Closeable {

    private static final List<Object> TYPES = List.of("line", "col", "type");
    private static final List<Object> ERRORS = List.of("line", "col", "node", "error");
    private static final List<Object> PACKAGES = List.of("line", "col", "package", "type");

    private final File fakeOutput;

    private final Map<String, CSVWriter> writers = new HashMap<>();

    public CsvReports(File fakeOutput) {
        this.fakeOutput = fakeOutput;
    }

    private void doWrite(CSVWriter writer, NodeWithRange<?> node, Object... args) {
        Position start = getStart(node);
        List<Object> objects = new ArrayList<>(List.of(start.line, start.column));
        objects.addAll(List.of(args));
        write(writer, objects.toArray());
    }

    protected final void writeRegular(NodeWithRange<?> node, Object... args) {
        doWrite(getWriter(), node, args);
    }

    protected final void writePackage(NodeWithRange<?> node, String packageName, String ofType) {
        doWrite(getPackagesWriter(), node, packageName, ofType);
    }


    protected final void writePackage(NodeWithRange<?> node, ClassOrInterfaceType packageName, ClassOrInterfaceType ofType) {
        writePackage(node, packageName.toString(), ofType.toString());
    }

    protected final void writeError(NodeWithRange<?> node, String actualNode, String error) {
        doWrite(getErrorWriter(), node, actualNode, error);
    }

    private CSVWriter getWriter(String which, List<Object> headers) {
        CSVWriter csvWriter = writers.get(which);
        if (csvWriter == null) {
            File actualFile = new File(fakeOutput.getAbsolutePath() + "." + which);
            actualFile.getParentFile().mkdirs();
            csvWriter = newCsvWriter(actualFile);
            writers.put(which, csvWriter);
            write(csvWriter, headers);
        }
        return csvWriter;
    }

    private CSVWriter getWriter() {
        return getWriter("types.csv", TYPES);
    }

    private CSVWriter getErrorWriter() {
        return getWriter("errors.csv", ERRORS);
    }

    private CSVWriter getPackagesWriter() {
        return getWriter("packages.csv", PACKAGES);
    }

    @Override
    public final void close() {
        for (CSVWriter writer : writers.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace(); // no biggie
            }
        }
    }

    public File getFakeOutput() {
        return fakeOutput;
    }
}
