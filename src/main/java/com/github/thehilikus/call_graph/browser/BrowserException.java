package com.github.thehilikus.call_graph.browser;

/**
 * Exception thrown when there is an error with the neo4j-browser
 */
public class BrowserException extends RuntimeException {
    public BrowserException(String msg) {
        super(msg);
    }

    public BrowserException(String msg, Exception e) {
        super(msg, e);
    }
}
