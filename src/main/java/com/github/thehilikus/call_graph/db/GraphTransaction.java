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
    private final Transaction neoTx;
    private final Map<String, Node> newNodes = new HashMap<>();
    private final Map<String, Relationship> newRelationships = new HashMap<>();

    GraphTransaction(Transaction transaction) {
        LOG.info("Starting DB transaction");
        this.neoTx = transaction;
    }

    public Node addNode(String id, String label, Map<String, Object> properties) {
        throwIfNoTransaction();
        Node node = neoTx.createNode(Label.label(label));
        properties.forEach(node::setProperty);
        newNodes.put(id, node);
        return node;
    }

    private void throwIfNoTransaction() {
        if (neoTx == null) {
            throw new GraphDatabaseException("Transaction not started");
        }
    }

    public boolean containsNode(String nodeId) {
        return newNodes.containsKey(nodeId);
    }


    public boolean containsRelationship(String relationshipId) {
        return newRelationships.containsKey(relationshipId);
    }

    public Node getNode(String nodeId) {
        return newNodes.get(nodeId);
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

    public Relationship addRelationship(String relationshipName, Node currentNode, Node targetNode) {
        throwIfNoTransaction();
        Relationship result = currentNode.createRelationshipTo(targetNode, RelationshipType.withName(relationshipName));
        String relationshipId = currentNode.getProperty(GraphConstants.ID) + "->" + targetNode.getProperty(GraphConstants.ID);
        newRelationships.put(relationshipId, result);

        return result;
    }

    public Relationship getRelationship(String relationshipId) {
        return newRelationships.get(relationshipId);
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
