package com.apixandru.java.visitors;

import com.github.javaparser.Range;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparing;

public class ChaChaVisitorContext {

    private static final Comparator<String> lineColComparator =
            comparing((String s) -> Long.valueOf(s.split(":", 2)[0]))
                    .thenComparing((String s) -> Long.valueOf(s.split(":", 2)[1].split(" ", 2)[0]));

    private final List<String> lines = new ArrayList<>();
    private final List<String> issues = new ArrayList<>();

    public void addLine(NodeWithRange<?> node, String line) {
        add(node, line, this.lines);
    }

    private void add(NodeWithRange<?> node, String line, List<String> lines) {
        String lineToAdd = range(node) + " " + line;
        lines.add(lineToAdd);
        System.out.println(lineToAdd);
    }

    public void addIssue(NodeWithRange<?> node, String line) {
        add(node, line, this.issues);
    }

    private String range(NodeWithRange<?> node) {
        Range range = node.getRange().get();
        String begin = range.begin.line + ":" + range.begin.column;
        String end = range.end.line + ":" + range.end.column;
        return begin;
    }

    public List<String> getLines() {
        lines.sort(lineColComparator);
        return lines;
    }

    public List<String> getIssues() {
        issues.sort(lineColComparator);
        return issues;
    }
}
