package com.github.thehilikus.call_graph.test.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppClass {
    private static final Logger LOG = LoggerFactory.getLogger(AppClass.class);

    public void appMethod() {
        System.out.println("[AppClass] appMethod");
        LOG.info("appMethod");
    }
}
