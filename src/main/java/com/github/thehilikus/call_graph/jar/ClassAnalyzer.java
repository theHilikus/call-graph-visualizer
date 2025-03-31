package com.github.thehilikus.call_graph.jar;

import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * POJO to navigate classes in the jar
 */
public class ClassAnalyzer extends ClassVisitor {
    private final String className;
    private final GraphTransaction activeTransaction;

    protected ClassAnalyzer(String className, GraphTransaction tx) {
        super(Opcodes.ASM9);
        this.className = className;
        this.activeTransaction = tx;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodNode methodNode = new MethodNode(api, access, name, descriptor, signature, exceptions);
        return new MethodAnalyzer(api, super.visitMethod(access, name, descriptor, signature, exceptions), className, methodNode, activeTransaction);
    }
}
