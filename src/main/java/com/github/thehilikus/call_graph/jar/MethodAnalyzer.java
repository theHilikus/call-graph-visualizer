package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * POJO to navigate method relationships
 */
public class MethodAnalyzer extends MethodVisitor {
    private static final String CONSTRUCTOR_NAME = "<init>";
    private final String className;
    private final GraphTransaction activeTransaction;
    private final String description;
    private final String methodName;

    protected MethodAnalyzer(int api, MethodVisitor methodVisitor, String className, MethodNode methodNode, GraphTransaction tx) {
        super(api, methodVisitor);
        this.className = className;
        this.methodName = methodNode.name;
        this.description = methodNode.desc;
        this.activeTransaction = tx;
    }

    private String getCleanName(String rawName) {
        String result = rawName;
        if (CONSTRUCTOR_NAME.equals(rawName)) {
            result = className.substring(className.lastIndexOf('.') + 1);
        }
        return result;
    }

    private String getCleanArguments() {
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

    @Override
    public void visitCode() {
        String signature = getCleanName(methodName) + getCleanArguments();
        if (activeTransaction.containsNode(className, signature)) {
            return;
        }

        activeTransaction.addNode(className, signature);
        super.visitCode();
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }
}
