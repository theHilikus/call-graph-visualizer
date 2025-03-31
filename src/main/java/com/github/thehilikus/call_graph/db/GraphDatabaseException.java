package com.github.thehilikus.call_graph.db;

/**
 * An exception in the persistence layer
 */
public class GraphDatabaseException extends RuntimeException {
    public GraphDatabaseException(String msg) {
        super(msg);
    }
}
