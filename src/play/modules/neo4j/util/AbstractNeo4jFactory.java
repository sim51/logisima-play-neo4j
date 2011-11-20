package play.modules.neo4j.util;

import models.relationship.EntityRelationType;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import play.Logger;
import play.modules.neo4j.annotation.Neo4jFactory;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jReflectionException;
import play.modules.neo4j.model.Neo4jModel;

public abstract class AbstractNeo4jFactory {

    /**
     * Reference node for all user.
     */
    private static Node               referenceNode;

    private static EntityRelationType root2ref;
    private static EntityRelationType ref2node;

    public final static String        NODE_KEY_COUNTER = "KEY_COUNTER";

    /**
     * Constructor of the User Factory.
     */
    public AbstractNeo4jFactory() {
        GraphDatabaseService graphDb = Neo4j.db();
        Neo4jFactory annotation = this.getClass().getAnnotation(Neo4jFactory.class);
        if (annotation != null && annotation.root2ref() != null && annotation.ref2node() != null) {
            this.root2ref = annotation.root2ref();
            this.ref2node = annotation.ref2node();
        }
        else {
            throw new Neo4jException(
                    "Factory class that extends AbstractNeo4jFactory must have the annotation @Neo4jFactory correctly configure !!!");
        }

        // if reference node doesn't exist, we create it
        if (!graphDb.getReferenceNode().hasRelationship(this.root2ref)) {
            Logger.info("Reference node doesn't exist for factory " + this.getClass().getSimpleName()
                    + ", we create it");
            Transaction tx = graphDb.beginTx();
            try {
                Node node = graphDb.createNode();
                node.setProperty(NODE_KEY_COUNTER, 1);
                graphDb.getReferenceNode().createRelationshipTo(node, this.root2ref);
                tx.success();
            } finally {
                tx.finish();
            }
        }

        // Retrieve the user reference node
        for (Relationship relationship : graphDb.getReferenceNode().getRelationships(this.root2ref, Direction.OUTGOING)) {
            this.referenceNode = relationship.getEndNode();
        }
    }

    /**
     * Method to get the next ID for Restaurent object.
     * 
     * @return
     */
    private synchronized long getNextId() {
        Long counter = null;
        try {
            counter = (Long) referenceNode.getProperty(NODE_KEY_COUNTER);
        } catch (NotFoundException e) {
            // Create a new counter
            counter = 0L;
        }
        referenceNode.setProperty(NODE_KEY_COUNTER, new Long(counter + 1));
        Logger.debug("New ID for factory " + this.getClass().getSimpleName() + " is " + counter);
        return counter;
    }

    /**
     * Method to save/update and index a node.
     * 
     * @param nodeWrapper
     * @return
     * @throws Neo4jReflectionException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Neo4jModel saveAndIndex(Neo4jModel nodeWrapper) throws Neo4jReflectionException {
        // if there is already an id, don't generate it
        if (nodeWrapper.getId() == null) {
            nodeWrapper.setId(getNextId());
        }

        Transaction tx = Neo4j.db().beginTx();
        try {
            // create the reference 2 node relationship
            referenceNode.createRelationshipTo(nodeWrapper.getNode(), this.ref2node);

            // create indexes ...
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {

                // create an index on the field ?
                Neo4jIndex nodeIndex = field.getAnnotation(Neo4jIndex.class);
                if (nodeIndex != null) {

                    // get the name of the index
                    String indexName = nodeIndex.value();
                    if (indexName.equals("")) {
                        indexName = nodeWrapper.getClass().getSimpleName() + "_" + field.getName();
                        indexName = indexName.toUpperCase();
                    }

                    // create the index
                    Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
                    indexNode.add(nodeWrapper.getNode(), field.getName(), field.get(nodeWrapper).toString());
                }
            }

            tx.success();
        } catch (IllegalArgumentException e) {
            throw new Neo4jReflectionException(e);
        } catch (IllegalAccessException e) {
            throw new Neo4jReflectionException(e);
        } finally {
            tx.finish();
        }
        return nodeWrapper;
    }
}
