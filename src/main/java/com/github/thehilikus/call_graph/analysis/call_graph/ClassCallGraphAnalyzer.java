package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.analysis.ClassAnalyzer;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;


/**
 * Analyzer to create a calling graph of classes and methods
 */
public class ClassCallGraphAnalyzer extends ClassAnalyzer {
    private final GraphTransaction activeTransaction;
    private final AnalysisFilter classFilter;
    private Node currentNode;


    protected ClassCallGraphAnalyzer(GraphTransaction tx, AnalysisFilter classFilter) {
        super(Opcodes.ASM9);
        this.activeTransaction = tx;
        this.classFilter = classFilter;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String className = name.replace("/", ".");
        if (classFilter.isClassIncluded(className)) {
            currentNode = activeTransaction.getNode(GraphConstants.Classes.CLASS_LABEL, className);
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (currentNode == null) {
            return null;
        }
        MethodNode methodNode = new MethodNode(api, access, name, descriptor, signature, exceptions);
        return new MethodCallGraphAnalyzer(api, super.visitMethod(access, name, descriptor, signature, exceptions), currentNode, methodNode, activeTransaction, classFilter);
    }
}
