package com.apixandru.java.visitors;

import java.io.File;
import java.util.List;

public class TypesCsvReport extends AbstractCsvReport {

    public TypesCsvReport(File fakeOutput) {
        super(fakeOutput, "types");
    }

    @Override
    protected List<Object> getWriterHeader() {
        return List.of("line", "col", "type");
    }

}
