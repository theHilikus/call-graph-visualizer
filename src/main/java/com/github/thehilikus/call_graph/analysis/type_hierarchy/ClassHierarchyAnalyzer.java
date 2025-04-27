package com.github.thehilikus.call_graph.analysis.type_hierarchy;

import com.github.thehilikus.call_graph.analysis.ClassAnalyzer;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Classes;
import com.github.thehilikus.call_graph.db.GraphConstants.Relations;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Analyzer to create a graph of inheritance and interface implementation for classes
 */
public class ClassHierarchyAnalyzer extends ClassAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(ClassHierarchyAnalyzer.class);

    private final Node jarNode;
    private final GraphTransaction activeTransaction;
    private final AnalysisFilter classFilter;

    public ClassHierarchyAnalyzer(Node jarNode, GraphTransaction tx, AnalysisFilter classFilter) {
        super(Opcodes.ASM9);
        this.jarNode = jarNode;
        this.activeTransaction = tx;
        this.classFilter = classFilter;
    }

    @Override
    public void visit(int version, int access, String name, String signature, @Nonnull String superName, @Nonnull String[] interfaces) {
        if ((access & Opcodes.ACC_MODULE) != 0) {
            //module descriptor, skip it
            return;
        }

        String className = name.replace("/", ".");
        if (classFilter.isClassIncluded(className)) {
            Node currentNode = createOrGetExistingClassNode(className);

            LOG.trace("Creating relationship '{}' between {} and {}", Relations.ARCHIVES, jarNode.getProperty(GraphConstants.FQN), currentNode.getProperty(GraphConstants.FQN));
            activeTransaction.addRelationship(Relations.ARCHIVES, jarNode, currentNode); //jar to class

            processSuperType(superName, currentNode);
            for (String interfaceName : interfaces) {
                processSuperType(interfaceName, currentNode);
            }
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    private Node createOrGetExistingClassNode(String className) {
        Node result = activeTransaction.getNode(Classes.CLASS_LABEL, className);
        if (result == null) {
            Map<String, Object> properties = Map.of(
                    GraphConstants.FQN, className,
                    Classes.SIMPLE_NAME, className.substring(className.lastIndexOf('.') + 1)
            );
            LOG.debug("Creating class node for {}", className);
            result = activeTransaction.addNode(Classes.CLASS_LABEL, properties);
        }

        return result;
    }

    private void processSuperType(String superName, Node currentNode) {
        String superClassName = superName.replace("/", ".");
        if (classFilter.isClassIncluded(superClassName)) {
            Node superClassNode = createOrGetExistingClassNode(superClassName);
            LOG.trace("Creating relationship '{}' between {} and {}", Relations.SUBTYPE, currentNode.getProperty(GraphConstants.FQN), superClassNode.getProperty(GraphConstants.FQN));
            activeTransaction.addRelationship(Relations.SUBTYPE, currentNode, superClassNode);
        }
    }
}
