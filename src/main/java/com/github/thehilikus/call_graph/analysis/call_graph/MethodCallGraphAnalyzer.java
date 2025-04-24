package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.analysis.JarAnalysisException;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Methods;
import com.github.thehilikus.call_graph.db.GraphConstants.Relations;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
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
    private final String descriptor;
    private final String methodName;
    private final int accessFlags;
    private final Node classNode;
    private Node currentNode;

    protected MethodCallGraphAnalyzer(int api, MethodVisitor methodVisitor, Node classNode, MethodNode methodNode, GraphTransaction tx, AnalysisFilter classFilter) {
        super(api, methodVisitor);
        this.classNode = classNode;
        this.methodName = methodNode.name;
        this.descriptor = methodNode.desc;
        this.accessFlags = methodNode.access;
        this.activeTransaction = tx;
        this.classFilter = classFilter;
    }

    /**
     * Called once for the current method
     */
    @Override
    public void visitCode() {
        currentNode = createMethodNode();

        LOG.trace("Creating relationship '{}' between {} and {}", Relations.CONTAINS, classNode.getProperty(GraphConstants.ID), currentNode.getProperty(GraphConstants.ID));
        activeTransaction.addRelationship(Relations.CONTAINS, classNode, currentNode);

        super.visitCode();
    }

    private Node createMethodNode() {
        String className = classNode.getProperty(GraphConstants.ID).toString();
        String signature = cleanMethodName(className, methodName) + buildArgumentsList(descriptor);
        return createOrGetExistingMethodNode(className, signature, accessFlags);
    }

    /**
     * Called for every call of the current method
     */
    @Override
    public void visitMethodInsn(int targetAccessFlags, String targetClassRaw, String targetMethodNameRaw, String descriptor, boolean isInterface) {
        if (currentNode == null) {
            throw new JarAnalysisException("Unknown current method with target method" + targetClassRaw + "#" + targetMethodNameRaw);
        }
        String targetClass = targetClassRaw.replace("/", ".");
        if (classFilter.isClassIncluded(targetClass)) {
            String targetMethod = cleanMethodName(targetClass, targetMethodNameRaw) + buildArgumentsList(descriptor);
            Node targetNode = createOrGetExistingMethodNode(targetClass, targetMethod, targetAccessFlags);
            Relationship relationship = createOrGetExistingRelationship(currentNode, targetNode);
            int currentCount = (int) relationship.getProperty(Relations.COUNT, 0);
            relationship.setProperty(Relations.COUNT, currentCount + 1);
        }

        super.visitMethodInsn(targetAccessFlags, targetClassRaw, targetMethodNameRaw, descriptor, isInterface);
    }

    private Node createOrGetExistingMethodNode(String nodeClass, String methodSignature, int accessFlags) {
        String nodeId = nodeClass + "#" + methodSignature;
        Node result = activeTransaction.getNode(Methods.METHOD_LABEL, nodeId);
        if (result == null) {
            boolean isStatic = (accessFlags & Opcodes.ACC_STATIC) != 0;
            Map<String, Object> properties = Map.of(
                    GraphConstants.ID, nodeId,
                    Methods.SIGNATURE, methodSignature,
                    Methods.STATIC, isStatic
            );
            LOG.trace("Creating method node for {}#{}", nodeClass, methodSignature);
            result = activeTransaction.addNode(Methods.METHOD_LABEL, properties);
        }

        return result;
    }

    private Relationship createOrGetExistingRelationship(Node currentNode, Node targetNode) {
        Relationship result = activeTransaction.getRelationship(Relations.CALLS, currentNode, targetNode);
        if (result == null) {
            LOG.trace("Creating relationship '{}' between {} and {}", Relations.CALLS, currentNode.getProperty(GraphConstants.ID), targetNode.getProperty(GraphConstants.ID));
            result = activeTransaction.addRelationship(Relations.CALLS, currentNode, targetNode);
        }

        return result;
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
