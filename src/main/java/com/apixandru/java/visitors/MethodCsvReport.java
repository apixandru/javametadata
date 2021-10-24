package com.apixandru.java.visitors;

import com.github.javaparser.Position;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;

import java.io.File;
import java.util.List;

import static com.apixandru.java.visitors.JpUtils.getStart;
import static java.util.stream.Collectors.toList;

public class MethodCsvReport extends AbstractCsvReport {

    public MethodCsvReport(File fakeOutput) {
        super(fakeOutput, "lines");
    }

    public void add(NodeWithRange<?> node, ResolvedReferenceTypeDeclaration declaration) {
        writeRegular(node, declaration.getQualifiedName(), "", "", "", "");
    }

    public void add(NodeWithRange<?> node, TypeDeclaration<?> owner, MethodDeclaration wrappedNode) {
        SimpleName wrappedNodeName = wrappedNode.getName();

        List<String> parameters = wrappedNode.getParameters()
                .stream()
                .map(JpUtils::getQualifiedName)
                .collect(toList());

        String fullyQualifiedOwnerName = owner.getFullyQualifiedName().get();

        add(node, wrappedNodeName, parameters, fullyQualifiedOwnerName);
    }

    public void add(NodeWithRange<?> node, SimpleName name, List<String> params, String fullyQualifiedOwnerName) {
        Position nameStart = getStart(name);

        writeRegular(node,
                fullyQualifiedOwnerName,
                name,
                String.join(",", params),
                nameStart.line,
                nameStart.column
        );
    }

    @Override
    protected List<Object> getWriterHeader() {
        return List.of("line", "col", "owner", "method", "argTypes", "refLine", "refCol");
    }

}
