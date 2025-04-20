package com.github.thehilikus.call_graph.test.core;

public class CoreClassChild extends CoreClassParent {
    public static void staticMethod() {
        System.out.println("[Child] staticMethod");
    }

    @Override
    public void override() {
        System.out.println("[Child] overridden");
    }

    @Override
    public void overrideCallsSuper() {
        System.out.println("[Child] overriddenCallsSuper");
        super.overrideCallsSuper();
        childOnly();
    }

    private void childOnly() {
        System.out.println("[Child] childOnly");
    }
}
