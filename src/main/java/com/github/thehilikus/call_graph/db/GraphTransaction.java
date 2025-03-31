package com.github.thehilikus.call_graph.db;

import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;

/**
 * POJO to represent a transaction in the database
 */
public class GraphTransaction implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GraphTransaction.class);
    private final Transaction transaction;
    private final Collection<String> newNodes = new HashSet<>();

    GraphTransaction(Transaction transaction) {
        LOG.info("Starting DB transaction");
        this.transaction = transaction;
    }

    public void addNode(String className, String methodSignature) {
        if (transaction == null) {
            throw new GraphDatabaseException("Transaction not started");
        }
        LOG.trace("Creating node for method {}#{}", className, methodSignature);

        Node node = transaction.createNode(Label.label(methodSignature));
        node.setProperty("class", className);
        newNodes.add(className + "#" + methodSignature);
    }

    public boolean containsNode(String className, String methodSignature) {
        return newNodes.contains(className + "#" + methodSignature);
    }

    public void commit() {
        if (transaction != null) {
            LOG.info("Committing DB transaction");
            transaction.commit();
        } else {
            throw new GraphDatabaseException("Transaction not started");
        }
    }

    public void rollback() {
        if (transaction != null) {
            LOG.info("Rolling back DB transaction");
            transaction.rollback();
        } else {
            throw new GraphDatabaseException("Transaction not started");
        }
    }

    @Override
    public void close() {
        if (transaction != null) {
            transaction.close();
        }
    }
}
