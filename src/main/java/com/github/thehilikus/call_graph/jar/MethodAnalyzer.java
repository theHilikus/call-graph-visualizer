package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * POJO to navigate method relationships
 */
public class MethodAnalyzer extends MethodVisitor {
    private static final String CONSTRUCTOR_NAME = "<init>";
    private static final String CALLS = "Calls";
    private final String className;
    private final GraphTransaction activeTransaction;
    private final String description;
    private final String methodName;
    private Node currentNode;

    protected MethodAnalyzer(int api, MethodVisitor methodVisitor, String className, MethodNode methodNode, GraphTransaction tx) {
        super(api, methodVisitor);
        this.className = className;
        this.methodName = methodNode.name;
        this.description = methodNode.desc;
        this.activeTransaction = tx;
    }

    /**
     * Called once for the current method
     */
    @Override
    public void visitCode() {
        String signature = cleanMethodName(className, methodName) + buildArgumentsList();
        currentNode = addOrGetNode(className, signature);

        super.visitCode();
    }

    /**
     * Called for every call of the current method
     */
    @Override
    public void visitMethodInsn(int opcode, String targetClassRaw, String targetMethodNameRaw, String descriptor, boolean isInterface) {
        if (currentNode == null) {
            throw new JarAnalysisException("Unknown current method with target method" + targetClassRaw + "#" + targetMethodNameRaw);
        }
        String targetClass = targetClassRaw.replace("/", ".");
        String targetMethod = cleanMethodName(targetClass, targetMethodNameRaw) + buildArgumentsList();
        Node targetNode = addOrGetNode(targetClass, targetMethod);
        activeTransaction.addRelationship(CALLS, currentNode, targetNode);

        super.visitMethodInsn(opcode, targetClassRaw, targetMethodNameRaw, descriptor, isInterface);
    }

    private Node addOrGetNode(String targetClass, String targetMethod) {
        Node result;
        if (!activeTransaction.containsMethodNode(targetClass, targetMethod)) {
            result = activeTransaction.addMethodNode(targetClass, targetMethod, false); //TODO: fix isStatic
        } else {
            result = activeTransaction.getMethodNode(targetClass, targetMethod);
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
}
