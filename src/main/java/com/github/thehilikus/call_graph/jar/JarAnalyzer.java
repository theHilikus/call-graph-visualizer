package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * POJO to navigate jar files extracting the information in them
 */
public class JarAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(JarAnalyzer.class);
    private final Path jarPath;


    public JarAnalyzer(Path jarPath) {
        if (!Files.exists(jarPath) || !Files.isReadable(jarPath)) {
            throw new IllegalArgumentException("jarPath does not exist or is not readable: " + jarPath);
        }
        if (!jarPath.getFileName().toString().endsWith(".jar")) {
            throw new IllegalArgumentException("jarPath is not a jar file: " + jarPath);
        }

        this.jarPath = jarPath;
    }

    public void process(GraphDatabase db) {
        LOG.info("Start processing {}", jarPath);
    }
}
