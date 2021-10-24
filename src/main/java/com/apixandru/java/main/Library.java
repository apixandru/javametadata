package com.apixandru.java.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class Library {

    private final List<File> children = new ArrayList<>();
    private final String root;

    private final String group;
    private final String artifact;
    private final String version;

    public Library(String root, String group, String artifact, String version, String... modules) {
        this.root = root;
        this.group = group;
        this.artifact = artifact;
        this.version = version;

        File rootFile = getFile(root);
        if (modules.length == 0) {
            children.add(rootFile);
        }
        for (String child : modules) {
            File file = new File(rootFile, child);
            children.add(file);
        }
    }

    public List<File> getSrcDirs() {
        return children;
    }

    private String buildFile(String root) {
        return getFile(root)
                .getAbsolutePath();
    }

    private File getFile(String root) {
        return new File(String.join("/", root, group, artifact, version));
    }

    public String getOutputDir(String relativeTo) {
        return buildFile(relativeTo);
    }

    public File getOutputFile(File inputFile, String targetDir) {
        List<File> file = children.stream()
                .filter(child -> inputFile.getAbsolutePath().startsWith(child.getAbsolutePath()))
                .map(child -> inputFile.getAbsolutePath().replace(child.getAbsolutePath(), targetDir))
                .map(File::new)
                .collect(toList());
        if (file.size() != 1) {
            throw new IllegalStateException("Unresolvable file " + inputFile);
        }
        return file.get(0);
    }
}
