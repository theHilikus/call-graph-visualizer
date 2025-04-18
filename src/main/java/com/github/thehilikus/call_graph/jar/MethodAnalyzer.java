package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Methods;
import com.github.thehilikus.call_graph.db.GraphConstants.Relations;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.Filter;
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
public class MethodAnalyzer extends MethodVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(MethodAnalyzer.class);
    private static final String CONSTRUCTOR_NAME = "<init>";
    private final GraphTransaction activeTransaction;
    private final Filter classFilter;
    private final String description;
    private final String methodName;
    private final int accessFlags;
    private final Node classNode;
    private Node currentNode;

    protected MethodAnalyzer(int api, MethodVisitor methodVisitor, Node classNode, MethodNode methodNode, GraphTransaction tx, Filter classFilter) {
        super(api, methodVisitor);
        this.classNode = classNode;
        this.methodName = methodNode.name;
        this.description = methodNode.desc;
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
        String signature = cleanMethodName(className, methodName) + buildArgumentsList();
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
        if (isClassIncluded(targetClass, classFilter)) {
            String targetMethod = cleanMethodName(targetClass, targetMethodNameRaw) + buildArgumentsList();
            Node targetNode = createOrGetExistingMethodNode(targetClass, targetMethod, targetAccessFlags);
            Relationship relationship = createOrGetExistingRelationship(currentNode, targetNode);
            int currentCount = (int) relationship.getProperty(Relations.COUNT, 0);
            relationship.setProperty(Relations.COUNT, currentCount + 1);
        }

        super.visitMethodInsn(targetAccessFlags, targetClassRaw, targetMethodNameRaw, descriptor, isInterface);
    }

    private Node createOrGetExistingMethodNode(String nodeClass, String methodSignature, int accessFlags) {
        Node result;
        String nodeId = nodeClass + "#" + methodSignature;
        if (!activeTransaction.containsNode(nodeId)) {
            boolean isStatic = (accessFlags & Opcodes.ACC_STATIC) != 0;
            Map<String, Object> properties = Map.of(
                    GraphConstants.ID, nodeId,
                    Methods.SIGNATURE, methodSignature,
                    Methods.STATIC, isStatic
            );
            LOG.trace("Creating method node for {}#{}", nodeClass, methodSignature);
            result = activeTransaction.addNode(nodeId, Methods.METHOD_LABEL, properties);
        } else {
            result = activeTransaction.getNode(nodeId);
        }
        return result;
    }

    private Relationship createOrGetExistingRelationship(Node currentNode, Node targetNode) {
        Relationship result;
        String relationshipId = currentNode.getProperty(GraphConstants.ID) + "->" + targetNode.getProperty(GraphConstants.ID);
        if (!activeTransaction.containsRelationship(relationshipId)) {
            LOG.trace("Creating relationship '{}' between {} and {}", Relations.CALLS, currentNode.getProperty(GraphConstants.ID), targetNode.getProperty(GraphConstants.ID));
            result = activeTransaction.addRelationship(Relations.CALLS, currentNode, targetNode);
        } else {
            result = activeTransaction.getRelationship(relationshipId);
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

    private String buildArgumentsList() {
        StringBuilder result = new StringBuilder("(");
        Type[] argumentTypes = Type.getArgumentTypes(description);
        for (int i = 0; i < argumentTypes.length; i++) {
            result.append(argumentTypes[i].getClassName());
            if (i < argumentTypes.length - 1) {
                result.append(", ");
            }
        }
        result.append(")");

        return result.toString();
    }

    static boolean isClassIncluded(String className, Filter classFilter) {
        for (String prefix : classFilter.exclude()) {
            if (className.startsWith(prefix)) {
                LOG.trace("Excluding class {}", className);
                return false;
            }
        }
        if (classFilter.include() != null) {
            for (String prefix : classFilter.include()) {
                if (className.startsWith(prefix)) {
                    LOG.trace("Including class {}", className);
                    return true;
                }
            }
            return false;
        }

        return true;
    }
}
