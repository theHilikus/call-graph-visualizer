package com.github.thehilikus.call_graph.db;

/**
 * Constants used in the database
 */
@SuppressWarnings("MissingJavadoc")
public class GraphConstants {
    public static final String ID = "id";

    public static class Jars {
        public static final String JAR_LABEL = "Jar";
    }

    public static class Classes {
        public static final String CLASS_LABEL = "Class";
        public static final String SIMPLE_NAME = "simpleClassName";
        public static final String FQN = "fullyQualifiedName";
    }

    public static class Methods {
        public static final String METHOD_LABEL = "Method";
        public static final String SIGNATURE = "signature";
        public static final String STATIC = "static";
    }

    public static class Relations {
        public static final String CALLS = "Calls";
        public static final String CONTAINS = "Contains";
        public static final String ARCHIVES = "Archives";
        public static final String COUNT = "count";
    }
}
