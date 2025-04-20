package com.github.thehilikus.call_graph.test.core;

public interface CoreInterface {
    void interfaceMethod();

    default void defaultMethod() {
        System.out.println("[CoreInterface] defaultMethod");
    }
}
