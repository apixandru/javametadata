package com.apixandru.java.main;

public class Libraries {

    private static final String rootSrcPath = "C:/_/workdir/sources";

    public static Library getHotspotLibrary() {
        return new Library(rootSrcPath, "JDK", "oracle-jdk8", "jdk-8u202-linux-x64");
    }

    public static Library getOpenJdkLibrary() {
        return new Library(rootSrcPath, "JDK", "openjdk8", "jdk8-b120",
                "corba", "jdk", "jaf", "jaxp", "jaxws", "nashorn", "langtools");
    }

    public static Library getCommonsLangLibrary() {
        return new Library(rootSrcPath, "commons-io", "commons-io", "2.11.0");
    }

}
