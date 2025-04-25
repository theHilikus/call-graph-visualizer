package com.github.thehilikus.call_graph.analysis.type_hierarchy;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;

/**
 * Creates a type hierarchy graph of all the jars provided
 */
public class TypeHierarchyCreator {
    private static final Logger LOG = LoggerFactory.getLogger(TypeHierarchyCreator.class);
    private final Set<Path> jarPaths;
    private final AnalysisFilter analysisFilter;

    public TypeHierarchyCreator(Set<Path> jarPaths, AnalysisFilter analysisFilter) {
        this.jarPaths = jarPaths;
        this.analysisFilter = analysisFilter;
    }

    public void run(GraphTransaction transaction) {
        LOG.info("Creating type hierarchy graph");
        for (Path jarPath : jarPaths) {
            JarTypeHierarchyAnalyzer jarTypeHierarchyAnalyzer = new JarTypeHierarchyAnalyzer(jarPath);
            jarTypeHierarchyAnalyzer.analyze(transaction, analysisFilter);
        }
    }
}
