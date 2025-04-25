package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.PerfTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

/**
 * Creates a call graph of all the jars provided
 */
public class CallGraphCreator {
    private static final Logger LOG = LoggerFactory.getLogger(CallGraphCreator.class);

    private final Set<Path> jarPaths;
    private final AnalysisFilter analysisFilter;

    public CallGraphCreator(Set<Path> jarPaths, AnalysisFilter analysisFilter) {
        this.jarPaths = jarPaths;
        this.analysisFilter = analysisFilter;
    }

    public void run(GraphTransaction transaction) {
        LOG.info("Creating call-graph");
        PerfTracker perfTracker = PerfTracker.createStarted("Call-graph creation");
        for (Path jarPath : jarPaths) {
            JarCallGraphAnalyzer jarCallGraphAnalyzer = new JarCallGraphAnalyzer(jarPath);
            jarCallGraphAnalyzer.analyze(transaction, analysisFilter);
        }
        DynamicBindingsAnalyzer dynamicBindingsAnalyzer = new DynamicBindingsAnalyzer(transaction);
        dynamicBindingsAnalyzer.analyze();
        perfTracker.finish();
    }
}
