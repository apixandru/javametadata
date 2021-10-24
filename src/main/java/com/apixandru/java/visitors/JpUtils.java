package com.apixandru.java.visitors;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.nodeTypes.NodeWithRange;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.Type;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Optional;

public final class JpUtils {

    private JpUtils() {
    }

    public static String getPackageName(CompilationUnit compilationUnit) {
        return compilationUnit.getPackageDeclaration()
                .map(PackageDeclaration::getNameAsString)
                .orElse(null);
    }

    static Optional<Node> findNodeForRange(Node node, Range tokenRange) {
        if (node.isPhantom()) {
            return Optional.empty();
        }
        if (node.getRange().isEmpty()) {
            return Optional.empty();
        }
        if (!node.getRange().get().contains(tokenRange)) {
            return Optional.empty();
        }

        for (Node child : node.getChildNodes()) {
            Optional<Node> found = findNodeForRange(child, tokenRange);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.of(node);
    }

    public static Position getStart(NodeWithRange<?> node) {
        return node.getRange()
                .get()
                .begin;
    }

    public static String getQualifiedName(Parameter param) {
        Type type = param.getType();
        if (type.isPrimitiveType()) {
            return type.toString();
        } else if (type.isArrayType()) {
            ArrayType at = (ArrayType) type;
            return at.getElementType() + "[]";
        }
        try {
            return type.asClassOrInterfaceType().resolve().getQualifiedName();
        } catch (Exception ex) {
            throw ex;
        }
    }

    static Node getNodeAt(int line, int column, CompilationUnit cu) {
        return getNodeAt(line, column, cu.findRootNode());
    }

    static Node getNodeAt(int line, int column, Node root) {
        if (root.getBegin().isEmpty() || root.getEnd().isEmpty()) {
            return null;
        }
        // Check cursor is in bounds
        // We won't instantly return null because the root range may be SMALLER than
        // the range of children.
        boolean bounds = true;
        Position cursor = new Position(line, column);
        if (cursor.isBefore(root.getBegin().get()) || cursor.isAfter(root.getEnd().get())) {
            bounds = false;
        }
        // Iterate over children, return non-null child
        for (Node child : root.getChildNodes()) {
            Node ret = getNodeAt(line, column, child);
            if (ret != null) {
                return ret;
            }
        }
        // If we're not in bounds and none of our children are THEN we assume this node is bad.
        if (!bounds) {
            return null;
        }
        // In bounds so we're good!
        return root;
    }

    public static CompilationUnit parse(JavaParser parser, File file) {
        try {
            ParseResult<CompilationUnit> result = parser.parse(file);
            if (result.isSuccessful()) {
                //noinspection OptionalGetWithoutIsPresent
                return result.getResult().get();
            }
            throw new ParseProblemException(result.getProblems());
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String findFilePath(CompilationUnit compilationUnit) {
        return findPath(compilationUnit)
                .toFile()
                .getAbsolutePath();
    }

    public static Path findPath(CompilationUnit compilationUnit) {
        return compilationUnit.getStorage()
                .orElseThrow(() -> new IllegalArgumentException("No storage for compilation unit!"))
                .getPath();
    }

}
