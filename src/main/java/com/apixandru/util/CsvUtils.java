package com.apixandru.util;

import com.opencsv.CSVWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class CsvUtils {

    private CsvUtils() {
    }

    public static void write(CSVWriter writer, Object... args) {
        write(writer, Arrays.asList(args));
    }

    public static void write(CSVWriter writer, List<Object> args) {
        String[] stringArgs = args.stream()
                .map(e -> e == null ? "" : e.toString())
                .toArray(String[]::new);

        writer.writeNext(stringArgs);
        writer.flushQuietly();
    }

    public static CSVWriter newCsvWriter(File file) {
        try {
            return new CSVWriter(new BufferedWriter(new FileWriter(file)));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

}
