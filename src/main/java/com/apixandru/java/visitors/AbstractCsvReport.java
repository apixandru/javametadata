package com.apixandru.java.visitors;

import com.github.javaparser.Position;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.opencsv.CSVWriter;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.apixandru.java.visitors.JpUtils.getStart;
import static com.apixandru.util.CsvUtils.newCsvWriter;
import static com.apixandru.util.CsvUtils.write;

public abstract class AbstractCsvReport implements Closeable {

    private final File errorsFile;
    private final File file;
    private final File packagesFile;

    private CSVWriter writer;
    private CSVWriter packagesWriter;
    private CSVWriter errorWriter;

    public AbstractCsvReport(File fakeOutput, String what) {
        this.file = new File(fakeOutput.getAbsolutePath() + "." + what + ".csv");
        this.packagesFile = new File(fakeOutput.getAbsolutePath() + "." + what + ".packages.csv");
        this.errorsFile = new File(fakeOutput.getAbsolutePath() + "." + what + ".errors.csv");
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

    protected abstract List<Object> getWriterHeader();

    private List<Object> getErrorWriterHeader() {
        return List.of("line", "col", "node", "error");
    }

    private List<Object> getPackagesWriterHeader() {
        return List.of("line", "col", "package", "type");
    }

    private CSVWriter getErrorWriter() {
        if (errorWriter == null) {
            errorsFile.getParentFile().mkdirs();
            errorWriter = newCsvWriter(errorsFile);
            write(errorWriter, getErrorWriterHeader().toArray());
        }
        return errorWriter;
    }

    private CSVWriter getWriter() {
        if (writer == null) {
            file.getParentFile().mkdirs();
            writer = newCsvWriter(this.file);
            write(writer, getWriterHeader().toArray());
        }
        return writer;
    }

    private CSVWriter getPackagesWriter() {
        if (packagesWriter == null) {
            file.getParentFile().mkdirs();
            packagesWriter = newCsvWriter(this.packagesFile);
            write(packagesWriter, getPackagesWriterHeader().toArray());
        }
        return packagesWriter;
    }

    @Override
    public final void close() {
        try (Closeable closeable1 = writer;
             Closeable closeable2 = errorWriter;
             Closeable closeable3 = packagesWriter) {
            // auto-close
        } catch (IOException e) {
            e.printStackTrace(); // no biggie
        }
    }

}
