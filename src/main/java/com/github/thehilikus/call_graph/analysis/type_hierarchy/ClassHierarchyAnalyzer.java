package com.github.thehilikus.call_graph.analysis.type_hierarchy;

import com.github.thehilikus.call_graph.analysis.ClassAnalyzer;
import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.analysis.AnalysisFilter;
import org.neo4j.graphdb.Node;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        String className = name.replace("/", ".");
        if (classFilter.isClassIncluded(className)) {
            LOG.debug("Creating class node for {}", className);
            Node currentNode = createClassNode(name);

            LOG.trace("Creating relationship '{}' between {} and {}", GraphConstants.Relations.ARCHIVES, jarNode.getProperty(GraphConstants.ID), currentNode.getProperty(GraphConstants.ID));
            activeTransaction.addRelationship(GraphConstants.Relations.ARCHIVES, jarNode, currentNode); //jar to class
        }

        super.visit(version, access, name, signature, superName, interfaces);
    }

    private Node createClassNode(String className) {
        Map<String, Object> properties = Map.of(
                GraphConstants.ID, className,
                GraphConstants.Classes.SIMPLE_NAME, className.substring(className.lastIndexOf('.') + 1)
        );
        return activeTransaction.addNode(GraphConstants.Classes.CLASS_LABEL, properties);
    }
}
