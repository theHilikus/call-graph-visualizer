package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.analysis.JarAnalysisException;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.PerfTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * POJO to navigate jar files extracting calling information between elements
 */
public class JarCallGraphAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(JarCallGraphAnalyzer.class);
    private final Path jarPath;

    public JarCallGraphAnalyzer(Path jarPath) {
        if (!Files.exists(jarPath) || !Files.isReadable(jarPath)) {
            throw new IllegalArgumentException("jarPath does not exist or is not readable: " + jarPath);
        }
        if (!jarPath.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("jarPath is not a jar file: " + jarPath);
        }

        this.jarPath = jarPath;
    }

    public void analyze(GraphTransaction tx, AnalysisFilter classFilter) {
        LOG.debug("Start processing jar {}", jarPath);
        PerfTracker perfTracker = PerfTracker.createStarted("Call-graph creation of '" + jarPath.getFileName() + "'");
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassCallGraphAnalyzer classHierarchyAnalyzer = new ClassCallGraphAnalyzer(tx, classFilter);
                    classHierarchyAnalyzer.analyze(jarFile, entry);
                }
            }
        } catch (IOException e) {
            throw new JarAnalysisException("Error processing jar file: " + jarPath, e);
        } finally {
            perfTracker.finish();
        }
    }
}
