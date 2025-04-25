package com.github.thehilikus.call_graph.analysis.call_graph;

/**
 * Exception to represent errors specific to call graph analysis
 */
public class CallGraphException extends RuntimeException {
    public CallGraphException(String msg) {
        super(msg);
    }
}
