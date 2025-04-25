package com.github.thehilikus.call_graph.run;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Tracks the execution time of specific operations and logs it under a single perf logger
 */
public class PerfTracker {
    private static final Logger LOG = LoggerFactory.getLogger(PerfTracker.class);
    private final StopWatch stopWatch;
    private final String operationTracked;

    public PerfTracker(String operationTracked) {
        this.operationTracked = operationTracked;
        stopWatch = new StopWatch();
    }

    public static PerfTracker createStarted(String operationTracked) {
        PerfTracker result = new PerfTracker(operationTracked);
        result.start();
        return result;
    }

    public void start() {
        stopWatch.start();
    }

    public void finish() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("{} finished in {} ms", operationTracked, stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }
}
