package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

/**
 * Analyzes dynamic bindings calls by resolving potential overrides and creating relationships in the graph database to reflect connections between methods
 */
public class DynamicBindingsAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(DynamicBindingsAnalyzer.class);
    private final GraphTransaction activeTransaction;

    public DynamicBindingsAnalyzer(GraphTransaction tx) {
        this.activeTransaction = tx;
    }

    public void start() {
        LOG.info("Start analyzing dynamic binding");
        try (Stream<Relationship> allDynamicCallsStream = activeTransaction.getAllRelationshipsWithProperty(GraphConstants.Relations.CALLS, GraphConstants.Relations.DYNAMIC, true)) {
            allDynamicCallsStream.forEach(dynamicCall -> {
                Collection<Node> allOverrides = findOverrides(dynamicCall.getEndNode());
                if (!allOverrides.isEmpty()) {
                    LOG.trace("Found {} overrides for dynamic call from {} to {}", allOverrides.size(), dynamicCall.getStartNode().getProperty(GraphConstants.FQN), dynamicCall.getEndNode().getProperty(GraphConstants.FQN));
                    allOverrides.forEach(override -> {
                        LOG.trace("Creating dynamic relationship '{}' between {} and {}", GraphConstants.Relations.CALLS, dynamicCall.getStartNode().getProperty(GraphConstants.FQN), override.getProperty(GraphConstants.FQN));
                        Relationship dynamicBindingRelation = activeTransaction.addRelationship(GraphConstants.Relations.CALLS, dynamicCall.getStartNode(), override);
                        dynamicBindingRelation.setProperty(GraphConstants.Relations.DYNAMIC, true);
                        int currentCount = (int) dynamicBindingRelation.getProperty(GraphConstants.Relations.COUNT, 0);
                        dynamicBindingRelation.setProperty(GraphConstants.Relations.COUNT, currentCount + 1);
                    });
                }
            });
        }
    }

    private Collection<Node> findOverrides(Node targetMethodNode) {
        var targetClassNode = targetMethodNode.getRelationships(Direction.INCOMING, RelationshipType.withName(GraphConstants.Relations.CONTAINS)).stream().findFirst()
                .orElseThrow(() -> new CallGraphException("No incoming class in method " + targetMethodNode.getProperty(GraphConstants.FQN)))
                .getStartNode();
        String targetMethodName = targetMethodNode.getProperty(GraphConstants.Methods.SIGNATURE).toString();

        Collection<Node> result = new ArrayList<>();
        for (Relationship typeRelation : targetClassNode.getRelationships(RelationshipType.withName(GraphConstants.Relations.SUBTYPE))) {
            Node subOrSuperTypeNode = typeRelation.getOtherNode(targetClassNode);
            String relatedClassName = subOrSuperTypeNode.getProperty(GraphConstants.FQN).toString();
            String relatedMethodId = relatedClassName + "#" + targetMethodName;
            Node relatedMethodNode = activeTransaction.getNode(GraphConstants.Methods.METHOD_LABEL, relatedMethodId);
            if (relatedMethodNode != null) {
                result.add(relatedMethodNode);
            }
        }

        return result;
    }
}