package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Jars;
import com.github.thehilikus.call_graph.db.GraphDatabase;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.Filter;
import org.apache.commons.lang3.time.StopWatch;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * POJO to navigate jar files extracting the information in them
 */
public class JarAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(JarAnalyzer.class);
    private final Path jarPath;
    private final boolean dryRun;


    public JarAnalyzer(Path jarPath, boolean dryRun) {
        if (!Files.exists(jarPath) || !Files.isReadable(jarPath)) {
            throw new IllegalArgumentException("jarPath does not exist or is not readable: " + jarPath);
        }
        if (!jarPath.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("jarPath is not a jar file: " + jarPath);
        }

        this.jarPath = jarPath;
        this.dryRun = dryRun;
    }

    public void process(GraphDatabase db, Filter classFilter) {
        if (dryRun) {
            LOG.warn("Running in dry-run mode. Changes won't be committed to the database");
        }

        StopWatch stopWatch = StopWatch.createStarted();
        LOG.info("==== Start processing jar {} ====", jarPath);
        try (JarFile jarFile = new JarFile(jarPath.toFile()); GraphTransaction tx = db.startTransaction()) {
            Node currentNode = createJarNode(tx);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(inputStream);
                        String className = entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6);
                        ClassAnalyzer classAnalyzer = new ClassAnalyzer(className, currentNode, tx, classFilter);
                        classReader.accept(classAnalyzer, 0);
                    } catch (IOException e) {
                        System.err.println("Error reading class file: " + entry.getName() + " - " + e.getMessage());
                    }
                }
            }
            if (!dryRun) {
                tx.commit();
            } else {
                tx.rollback();
            }

            LOG.info("Done processing jar {}: {} nodes in graph processed in {} ms\n", jarPath.getFileName(), tx.getNodeCount(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
            throw new JarAnalysisException("Error processing jar file: " + jarPath, e);
        }
    }

    private Node createJarNode(GraphTransaction activeTransaction) {
        String jarName = jarPath.getFileName().toString();
        Map<String, Object> properties = Map.of(
                GraphConstants.ID, jarName
        );
        LOG.debug("Creating node for jar {}", jarName);
        return activeTransaction.addNode(jarName, Jars.JAR_LABEL, properties);
    }
}
