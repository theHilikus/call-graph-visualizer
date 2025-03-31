package com.github.thehilikus.call_graph.db;

import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * POJO to represent a transaction in the database
 */
public class GraphTransaction implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraphTransaction.class);
    private static final String METHOD_LABEL = "Method";
    private static final String FQN = "fullQualifiedClassName";
    private static final String SIMPLE_NAME = "simpleClassName";
    private static final String SIGNATURE = "signature";
    private static final String STATIC = "static";
    private final Transaction neoTx;
    private final Map<String, Node> newNodes = new HashMap<>();

    GraphTransaction(Transaction transaction) {
        LOG.info("Starting DB transaction");
        this.neoTx = transaction;
    }

    public Node addMethodNode(String className, String methodSignature, boolean isStatic) {
        throwIfNoTransaction();
        LOG.trace("Creating node for method {}#{}", className, methodSignature);

        Node node = neoTx.createNode(Label.label(METHOD_LABEL));
        node.setProperty(FQN, className);
        node.setProperty(SIMPLE_NAME, className.substring(className.lastIndexOf(".") + 1));
        node.setProperty(SIGNATURE, methodSignature);
        node.setProperty(STATIC, isStatic);
        newNodes.put(className + "#" + methodSignature, node);

        return node;
    }

    private void throwIfNoTransaction() {
        if (neoTx == null) {
            throw new GraphDatabaseException("Transaction not started");
        }
    }

    public boolean containsMethodNode(String className, String methodSignature) {
        return newNodes.containsKey(className + "#" + methodSignature);
    }


    public Node getMethodNode(String className, String methodSignature) {
        return newNodes.get(className + "#" + methodSignature);
    }

    public void commit() {
        throwIfNoTransaction();

        LOG.info("Committing DB transaction");
        neoTx.commit();
    }

    public void rollback() {
        throwIfNoTransaction();

        LOG.info("Rolling back DB transaction");
        neoTx.rollback();
    }

    public void addRelationship(String relationshipName, Node currentNode, Node targetNode) {
        throwIfNoTransaction();
        LOG.trace("Creating relationship {} between {}#{} and {}#{}", relationshipName, currentNode.getProperty(SIMPLE_NAME), currentNode.getProperty(SIGNATURE), targetNode.getProperty(SIMPLE_NAME), targetNode.getProperty(SIGNATURE));
        currentNode.createRelationshipTo(targetNode, RelationshipType.withName(relationshipName));
    }

    public int getNodeCount() {
        throwIfNoTransaction();
        return newNodes.size();
    }

    @Override
    public void close() {
        if (neoTx != null) {
            neoTx.close();
        }
    }
}
