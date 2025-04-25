package com.github.thehilikus.call_graph.analysis;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Abstract class for analyzing class files within a jar entry using ASM.
 * Processes class files to facilitate further analysis by subclasses.
 */
public abstract class ClassAnalyzer extends ClassVisitor {
    protected ClassAnalyzer(int api) {
        super(api);
    }

    /**
     * Reads and analyzes a class file from a specified jar entry.
     *
     * @param jarFile the jar file containing the class
     * @param entry   the specific jar entry representing the class file
     */
    public void start(JarFile jarFile, JarEntry entry) {
        try (InputStream inputStream = jarFile.getInputStream(entry)) {
            ClassReader classReader = new ClassReader(inputStream);
            classReader.accept(this, 0);
        } catch (IOException e) {
            throw new JarAnalysisException("Error reading class file: " + entry.getName(), e);
        }
    }
}
