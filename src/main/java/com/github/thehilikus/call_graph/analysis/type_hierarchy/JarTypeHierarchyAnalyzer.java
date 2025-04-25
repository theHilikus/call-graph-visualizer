package com.github.thehilikus.call_graph.analysis.type_hierarchy;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.analysis.JarAnalysisException;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Jars;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
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

    public void analyze(GraphTransaction tx, AnalysisFilter classFilter) {
        LOG.debug("Start processing jar {}", jarPath);
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            Node currentNode = createJarNode(tx);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    ClassHierarchyAnalyzer classHierarchyAnalyzer = new ClassHierarchyAnalyzer(currentNode, tx, classFilter);
                    classHierarchyAnalyzer.analyze(jarFile, entry);
                }
            }
        } catch (IOException e) {
            throw new JarAnalysisException("Error processing jar file: " + jarPath, e);
        }
    }

    private Node createJarNode(GraphTransaction activeTransaction) {
        String jarName = jarPath.getFileName().toString();
        Map<String, Object> properties = Map.of(
                GraphConstants.FQN, jarName
        );
        LOG.debug("Creating jar node for {}", jarName);
        return activeTransaction.addNode(Jars.JAR_LABEL, properties);
    }
}
