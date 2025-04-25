package com.github.thehilikus.call_graph.analysis.call_graph;

import com.github.thehilikus.call_graph.db.GraphConstants;
import com.github.thehilikus.call_graph.db.GraphConstants.Classes;
import com.github.thehilikus.call_graph.db.GraphConstants.Methods;
import com.github.thehilikus.call_graph.db.GraphConstants.Relations;
import com.github.thehilikus.call_graph.db.GraphTransaction;
import com.github.thehilikus.call_graph.run.PerfTracker;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
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

    public void analyze() {
        LOG.info("Start analyzing dynamic binding");
        PerfTracker perfTracker = PerfTracker.createStarted("Dynamic binding analysis");
        try (Stream<Relationship> allDynamicCalls = activeTransaction.getAllRelationshipsWithProperty(Relations.CALLS, Relations.DYNAMIC, true)) {
            allDynamicCalls.forEach(dynamicCall -> {
                Collection<Node> newCalls = processDynamicCalls(dynamicCall.getEndNode());
                newCalls.forEach(newCall -> {
                    LOG.trace("Creating dynamic relationship '{}' between {} and {}", Relations.CALLS, dynamicCall.getStartNode().getProperty(GraphConstants.FQN), newCall.getProperty(GraphConstants.FQN));
                    Relationship dynamicBindingRelation = activeTransaction.addRelationship(Relations.CALLS, dynamicCall.getStartNode(), newCall);
                    dynamicBindingRelation.setProperty(Relations.DYNAMIC, true);
                    int currentCount = (int) dynamicBindingRelation.getProperty(Relations.COUNT, 0);
                    dynamicBindingRelation.setProperty(Relations.COUNT, currentCount + 1);
                });
            });
        } finally {
            perfTracker.finish();
        }
    }

    @Nonnull
    private Collection<Node> processDynamicCalls(Node targetMethodNode) {
        Optional<Relationship> contains = targetMethodNode.getRelationships(Direction.INCOMING, RelationshipType.withName(Relations.CONTAINS)).stream().findFirst();
        Collection<Node> result;
        if (contains.isPresent()) {
            String targetMethodName = targetMethodNode.getProperty(Methods.SIGNATURE).toString();
            Node targetClassNode = contains.get().getStartNode();
            result = findOverrides(targetMethodName, targetClassNode);
            if (!result.isEmpty() && (boolean) targetMethodNode.getProperty(Methods.ABSTRACT, false)) {
                targetMethodNode.getRelationships(Direction.INCOMING, RelationshipType.withName(Relations.CALLS)).forEach(Relationship::delete);
            }
        } else {
            //pointing to method that is not contained by any class. This is caused by calls to parent methods via children references
            result = fixCallsDoneToParentMethodsViaChildren(targetMethodNode);
        }

        return result;
    }

    private Collection<Node> findOverrides(String targetMethodName, Node targetClassNode) {
        Collection<Node> result = new ArrayList<>();
        for (Relationship typeRelation : targetClassNode.getRelationships(RelationshipType.withName(Relations.SUBTYPE))) {
            Node subOrSuperTypeNode = typeRelation.getOtherNode(targetClassNode);
            String relatedClassName = subOrSuperTypeNode.getProperty(GraphConstants.FQN).toString();
            String relatedMethodId = relatedClassName + "#" + targetMethodName;
            Node relatedMethodNode = activeTransaction.getNode(Methods.METHOD_LABEL, relatedMethodId);
            if (relatedMethodNode != null) {
                result.add(relatedMethodNode);
            }
        }

        return result;
    }

    private Collection<Node> fixCallsDoneToParentMethodsViaChildren(Node targetMethodNode) {
        String targetMethodFullyQualifiedName = targetMethodNode.getProperty(GraphConstants.FQN).toString();
        String targetMethodClass = targetMethodFullyQualifiedName.substring(0, targetMethodFullyQualifiedName.lastIndexOf('#'));
        String targetMethodName = targetMethodFullyQualifiedName.substring(targetMethodFullyQualifiedName.lastIndexOf('#') + 1);
        Node methodClassNode = activeTransaction.getNode(Classes.CLASS_LABEL, targetMethodClass);

        Collection<Node> result = new ArrayList<>();
        Node parentClass = methodClassNode.getSingleRelationship(RelationshipType.withName(Relations.SUBTYPE), Direction.OUTGOING).getEndNode();
        while (parentClass != null) {
            String parentMethodId = parentClass.getProperty(GraphConstants.FQN) + "#" + targetMethodName;
            Node parentMethodNode = activeTransaction.getNode(Methods.METHOD_LABEL, parentMethodId);
            if (parentMethodNode != null) {
                result.add(parentMethodNode);
                LOG.debug("Replacing overridden call {} that doesn't contain the method. Proper containing class is {}", targetMethodFullyQualifiedName, parentClass.getProperty(GraphConstants.FQN));
                targetMethodNode.getRelationships().forEach(Relationship::delete);
                targetMethodNode.delete();
                break;
            }
            parentClass = parentClass.getSingleRelationship(RelationshipType.withName(Relations.SUBTYPE), Direction.OUTGOING).getEndNode();
        }

        return result;
    }
}