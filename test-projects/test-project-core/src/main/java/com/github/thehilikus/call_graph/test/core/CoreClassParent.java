package com.github.thehilikus.call_graph.test.core;

public class CoreClassParent {
    public final void regularParentMethod() {
        System.out.println("[parent] regularMethod");
    }

    public void override() {
        System.out.println("[parent] overridden");
    }

    public void overrideCallsSuper() {
        System.out.println("[parent] overriddenCallsSuper");
    }
}
