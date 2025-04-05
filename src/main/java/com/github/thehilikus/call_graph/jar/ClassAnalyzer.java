package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Classes;
import com.github.thehilikus.call_graph.db.GraphTransaction;
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
    private final GraphTransaction activeTransaction;
    private Node currentNode;


    protected ClassAnalyzer(String className, GraphTransaction tx) {
        super(Opcodes.ASM9);
        this.className = className;
        this.activeTransaction = tx;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Map<String, Object> properties = Map.of(
                GraphConstants.ID, className,
                Classes.SIMPLE_NAME, className.substring(className.lastIndexOf('.') + 1)
        );
        LOG.debug("Creating node for class {}", className);
        currentNode = activeTransaction.addNode(className, Classes.CLASS_LABEL, properties);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodNode methodNode = new MethodNode(api, access, name, descriptor, signature, exceptions);
        return new MethodAnalyzer(api, super.visitMethod(access, name, descriptor, signature, exceptions), className, methodNode, activeTransaction);
    }
}
