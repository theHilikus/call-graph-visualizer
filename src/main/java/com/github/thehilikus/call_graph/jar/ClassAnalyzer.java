package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Classes;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.Filter;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * POJO to navigate classes in the jar
 */
public class ClassAnalyzer extends ClassVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(ClassAnalyzer.class);
    private final String className;
    private final Node jarNode;
    private final GraphTransaction activeTransaction;
    private final Filter classFilter;
    private Node currentNode;


    protected ClassAnalyzer(String className, Node jarNode, GraphTransaction tx, Filter classFilter) {
        super(Opcodes.ASM9);
        this.className = className;
        this.jarNode = jarNode;
        this.activeTransaction = tx;
        this.classFilter = classFilter;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (MethodAnalyzer.isClassIncluded(className, classFilter)) {
            LOG.debug("Creating class node for {}", className);
            currentNode = createClassNode();

            LOG.trace("Creating relationship '{}' between {} and {}", GraphConstants.Relations.ARCHIVES, jarNode.getProperty(GraphConstants.ID), currentNode.getProperty(GraphConstants.ID));
            activeTransaction.addRelationship(GraphConstants.Relations.ARCHIVES, jarNode, currentNode); //jar to class
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    private Node createClassNode() {
        Map<String, Object> properties = Map.of(
                GraphConstants.ID, className,
                Classes.SIMPLE_NAME, className.substring(className.lastIndexOf('.') + 1)
        );
        return activeTransaction.addNode(className, Classes.CLASS_LABEL, properties);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        if (currentNode == null) {
            return null;
        }
        MethodNode methodNode = new MethodNode(api, access, name, descriptor, signature, exceptions);
        return new MethodAnalyzer(api, super.visitMethod(access, name, descriptor, signature, exceptions), currentNode, methodNode, activeTransaction, classFilter);
    }
}
