package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphDatabase;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

    public void process(GraphDatabase db, boolean dryRun) {
        if (dryRun) {
            LOG.warn("Running in dry-run mode. Changes won't be committed to the database");
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile()); GraphTransaction tx = db.startTransaction()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        ClassReader classReader = new ClassReader(inputStream);
                        String className = entry.getName().replace("/", ".").substring(0, entry.getName().length() - 6);
                        LOG.debug("Processing class {}", className);
                        ClassAnalyzer classAnalyzer = new ClassAnalyzer(className, tx);
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
        } catch (IOException e) {
            throw new JarAnalysisException("Error processing jar file: " + jarPath, e);
        }
    }
}
