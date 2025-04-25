package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Methods;
import com.github.thehilikus.call_graph.db.GraphConstants.Relations;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * POJO to navigate method relationships
 */
public class MethodCallGraphAnalyzer extends MethodVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(MethodCallGraphAnalyzer.class);
    private static final String CONSTRUCTOR_NAME = "<init>";
    private final GraphTransaction activeTransaction;
    private final AnalysisFilter classFilter;
    private final Node currentMethodNode;

    protected MethodCallGraphAnalyzer(int api, MethodVisitor methodVisitor, Node classNode, MethodNode methodNode, GraphTransaction tx, AnalysisFilter classFilter) {
        super(api, methodVisitor);
        this.activeTransaction = tx;
        this.classFilter = classFilter;

        String className = classNode.getProperty(GraphConstants.FQN).toString();
        String methodSignature = cleanMethodName(className, methodNode.name) + buildArgumentsList(methodNode.desc);
        String nodeId = className + "#" + methodSignature;
        currentMethodNode = createOrGetExistingMethodNode(nodeId);
        addMethodNodeProperties(currentMethodNode, nodeId, methodNode.access);

        LOG.trace("Creating relationship '{}' between {} and {}", Relations.CONTAINS, className, currentMethodNode.getProperty(GraphConstants.FQN));
        activeTransaction.addRelationship(Relations.CONTAINS, classNode, currentMethodNode);
    }

    /**
     * Called for every method call of the current method
     */
    @Override
    public void visitMethodInsn(int opcode, String targetClassRaw, String targetMethodNameRaw, String descriptor, boolean isInterface) {
        if (currentMethodNode == null) {
            throw new CallGraphException("Unknown current method with target method" + targetClassRaw + "#" + targetMethodNameRaw);
        }
        String targetClass = targetClassRaw.replace("/", ".");
        if (classFilter.isClassIncluded(targetClass)) {
            String targetMethod = cleanMethodName(targetClass, targetMethodNameRaw) + buildArgumentsList(descriptor);
            String targetNodeId = targetClass + "#" + targetMethod;
            Node targetNode = createOrGetExistingMethodNode(targetNodeId);
            addMethodNodeProperties(targetNode, targetNodeId, opcode);

            Relationship relationship = createOrGetExistingRelationship(currentMethodNode, targetNode);
            addRelationshipProperties(relationship, opcode);
        }

        super.visitMethodInsn(opcode, targetClassRaw, targetMethodNameRaw, descriptor, isInterface);
    }

    private Node createOrGetExistingMethodNode(String nodeId) {
        Node result = activeTransaction.getNode(Methods.METHOD_LABEL, nodeId);
        if (result == null) {
            LOG.trace("Creating method node for {}", nodeId);
            result = activeTransaction.addNode(Methods.METHOD_LABEL, Map.of(GraphConstants.FQN, nodeId));
        }

        return result;
    }

    private static void addMethodNodeProperties(Entity methodNode, String nodeId, int opcode) {
        boolean isStatic = (opcode & Opcodes.ACC_STATIC) != 0;
        String signature = nodeId.substring(nodeId.lastIndexOf('#') + 1);
        methodNode.setProperty(Methods.STATIC, isStatic);
        methodNode.setProperty(Methods.SIGNATURE, signature);
    }

    private Relationship createOrGetExistingRelationship(Node currentNode, Node targetNode) {
        Relationship result = activeTransaction.getRelationship(Relations.CALLS, currentNode, targetNode);
        if (result == null) {
            LOG.trace("Creating relationship '{}' between {} and {}", Relations.CALLS, currentNode.getProperty(GraphConstants.FQN), targetNode.getProperty(GraphConstants.FQN));

            result = activeTransaction.addRelationship(Relations.CALLS, currentNode, targetNode);
        }

        return result;
    }

    private void addRelationshipProperties(Relationship relationship, int opcode) {
        boolean isDynamic = opcode == Opcodes.INVOKEVIRTUAL
                || opcode == Opcodes.INVOKEINTERFACE
                || opcode == Opcodes.INVOKEDYNAMIC;
        relationship.setProperty(Relations.DYNAMIC, isDynamic);
        int currentCount = (int) relationship.getProperty(Relations.COUNT, 0);
        relationship.setProperty(Relations.COUNT, currentCount + 1);
    }

    private String cleanMethodName(String className, String rawMethodName) {
        String result = rawMethodName;
        if (CONSTRUCTOR_NAME.equals(rawMethodName)) {
            result = className.substring(className.lastIndexOf('.') + 1);
        }
        return result;
    }

    private String buildArgumentsList(String methodDescriptor) {
        StringBuilder result = new StringBuilder("(");
        Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
        for (int i = 0; i < argumentTypes.length; i++) {
            result.append(argumentTypes[i].getClassName());
            if (i < argumentTypes.length - 1) {
                result.append(", ");
            }
        }
        result.append(")");

        return result.toString();
    }
}
