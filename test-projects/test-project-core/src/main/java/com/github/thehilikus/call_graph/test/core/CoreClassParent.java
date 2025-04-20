package com.github.thehilikus.call_graph.test.core;

public class CoreClassParent {
    public int regularMethod() {
        System.out.println("[parent] regularMethod");
        return 1;
    }

    public void override() {
        System.out.println("[parent] overridden");
    }

    public void overrideCallsSuper() {
        System.out.println("[parent] overriddenCallsSuper");
    }
}
