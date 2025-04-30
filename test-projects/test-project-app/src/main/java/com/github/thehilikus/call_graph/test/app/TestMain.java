package com.github.thehilikus.call_graph.test.app;

import com.github.thehilikus.call_graph.test.core.CoreClassChild;
import com.github.thehilikus.call_graph.test.core.CoreClassParent;
import com.github.thehilikus.call_graph.test.core.CoreInterface;
import com.github.thehilikus.call_graph.test.core.CoreInterfaceImpl;

public class TestMain {
    public static void main(String[] args) {
        new TestMain().start();

    }

    private void start() {
        sameJar();
        acrossJars();
        moreCalls();
    }

    private void sameJar() {
        AppClass appClass = new AppClass();
        appClass.appMethod();
        appClass.appMethod();
    }

    private void acrossJars() {
        CoreClassParent coreClassParent = new CoreClassParent();
        coreClassParent.override();
        coreClassParent.overrideCallsSuper();
        System.out.println("=========");
        CoreClassParent child = new CoreClassChild();
        child.override();
        child.overrideCallsSuper();
        System.out.println("=========");
        CoreClassChild.staticMethod();
        System.out.println("=========");
        CoreInterface coreInterface = new CoreInterfaceImpl();
        coreInterface.interfaceMethod();
        coreInterface.defaultMethod();
    }

    private void moreCalls() {
        System.out.println("=========");
        CoreClassChild child = new CoreClassChild();
        child.regularParentMethod();
        CoreInterface coreInterface = new CoreInterfaceImpl();
        coreInterface.interfaceMethod();
    }
}
