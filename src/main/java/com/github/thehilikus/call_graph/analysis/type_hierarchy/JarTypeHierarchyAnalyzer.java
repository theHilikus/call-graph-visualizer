package com.github.thehilikus.call_graph.analysis.type_hierarchy;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.analysis.JarAnalysisException;
import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * POJO to navigate jar files extracting type hierarchy between elements
 */
public class JarTypeHierarchyAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(JarTypeHierarchyAnalyzer.class);

    private final Path jarPath;

    public JarTypeHierarchyAnalyzer(Path jarPath) {
        if (!Files.exists(jarPath) || !Files.isReadable(jarPath)) {
            throw new IllegalArgumentException("jarPath does not exist or is not readable: " + jarPath);
        }
        if (!jarPath.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("jarPath is not a jar file: " + jarPath);
        }

        this.jarPath = jarPath;
    }

    public void process(GraphTransaction tx, AnalysisFilter classFilter) {
        StopWatch stopWatch = StopWatch.createStarted();
        LOG.debug("Start processing jar {}", jarPath);
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Node currentNode = createJarNode(tx);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassHierarchyAnalyzer classHierarchyAnalyzer = new ClassHierarchyAnalyzer(currentNode, tx, classFilter);
                    classHierarchyAnalyzer.start(jarFile, entry);
                }
            }
            LOG.debug("Done processing jar {}: Processed in {} ms", jarPath.getFileName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            throw new JarAnalysisException("Error processing jar file: " + jarPath, e);
        }
    }

    private Node createJarNode(GraphTransaction activeTransaction) {
        String jarName = jarPath.getFileName().toString();
        Map<String, Object> properties = Map.of(
                GraphConstants.ID, jarName
        );
        LOG.debug("Creating jar node for {}", jarName);
        return activeTransaction.addNode(GraphConstants.Jars.JAR_LABEL, properties);
    }
}
