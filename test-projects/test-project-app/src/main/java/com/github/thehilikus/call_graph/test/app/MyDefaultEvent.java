package com.github.thehilikus.call_graph.test.app;

import org.slf4j.event.DefaultLoggingEvent;

public class MyDefaultEvent extends DefaultLoggingEvent {
    public MyDefaultEvent() {
        super(null, null);
        addMarker(null);
    }
}
