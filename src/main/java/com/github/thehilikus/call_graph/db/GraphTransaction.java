package com.github.thehilikus.call_graph.db;

import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import java.util.Map;
import java.util.stream.Stream;

/**
 * POJO to represent a transaction in the database
 */
public class GraphTransaction implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraphTransaction.class);
    private final Transaction neoTx;

    GraphTransaction(Transaction transaction) {
        LOG.info("Starting DB transaction");
        this.neoTx = transaction;
    }

    public Node addNode(String label, Map<String, Object> properties) {
        throwIfNoTransaction();
        Node node = neoTx.createNode(Label.label(label));
        properties.forEach(node::setProperty);
        return node;
    }

    private void throwIfNoTransaction() {
        if (neoTx == null) {
            throw new GraphDatabaseException("Transaction not started");
        }
    }

    public Node getNode(String label, String nodeId) {
        return neoTx.findNode(Label.label(label), GraphConstants.FQN, nodeId);
    }

    public void commit() {
        throwIfNoTransaction();

        LOG.info("Committing DB transaction with {} nodes", getNodeCount());
        neoTx.commit();
    }

    public void rollback() {
        throwIfNoTransaction();

        LOG.info("Rolling back DB transaction with {} nodes", getNodeCount());
        neoTx.rollback();
    }

    public Relationship addRelationship(String relationshipName, Node currentNode, Node targetNode) {
        throwIfNoTransaction();

        return currentNode.createRelationshipTo(targetNode, RelationshipType.withName(relationshipName));
    }

    @CheckForNull
    public Relationship getRelationship(String relationshipType, Node sourceNode, Node targetNode) {
        Iterable<Relationship> relationships = sourceNode.getRelationships(RelationshipType.withName(relationshipType));
        for (Relationship relationship : relationships) {
            if (relationship.getEndNode().equals(targetNode)) {
                return relationship;
            }
        }

        return null;
    }

    public Stream<Relationship> getAllRelationshipsWithProperty(String relationshipType, String propertyName, Object propertyValue) {
        Result relationshipResults = neoTx.execute("MATCH (n)-[r:" + relationshipType + "]->(m) WHERE r." + propertyName + " = " + propertyValue + " RETURN r");
        return relationshipResults.stream().map(result -> (Relationship) result.get("r"));
    }

    private long getNodeCount() {
        throwIfNoTransaction();
        return (long) neoTx.execute("MATCH (n) RETURN count(n) as count").next().get("count");
    }

    @Override
    public void close() {
        if (neoTx != null) {
            neoTx.close();
        }
    }
}
