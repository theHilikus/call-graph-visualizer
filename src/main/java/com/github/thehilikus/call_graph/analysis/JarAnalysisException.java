package com.github.thehilikus.call_graph.analysis;

/**
 * Exception thrown when there is an error processing a jar
 */
public class JarAnalysisException extends RuntimeException {
    public JarAnalysisException(String msg) {
        super(msg);
    }

    public JarAnalysisException(String msg, Exception e) {
        super(msg, e);
    }
}
