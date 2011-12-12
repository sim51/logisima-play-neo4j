package play.modules.neo4j.util;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import play.Logger;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;

import java.util.HashMap;
import java.util.Map;

public class Neo4jFactory {

    private static String className;

    /**
     * Reference node for all object.
     */
    private static Node referenceNode;

    /**
     * Define the relationshipType between the root node, and the model reference node.
     */
    private static RelationshipType root2ref;

    /**
     * Define the relationshipType between the model reference node and models.
     */
    private static RelationshipType ref2node;

    /**
     * Name of the key property on each object.
     */
    public final static String NODE_KEY_COUNTER = "KEY_COUNTER";
    private Class clazz;
    private final static String REFERENCE_KEYWORD = "_REF";


    public Neo4jFactory(Class clazz) {
        this.clazz = clazz;
        GraphDatabaseService graphDb = Neo4j.db();

        if (this.clazz != null && this.clazz.getSimpleName() != null ) {
            String className = this.clazz.getSimpleName().toUpperCase();
            this.root2ref = DynamicRelationshipType.withName(className+ REFERENCE_KEYWORD);
            this.ref2node = DynamicRelationshipType.withName(className);
        } else {
            throw new Neo4jPlayException(
                    "Factory class that extends AbstractNeo4jFactory must have the annotation @Neo4jFactory correctly configure !!!");
        }

        // if reference node doesn't exist, we create it
        if (!graphDb.getReferenceNode().hasRelationship(this.root2ref)) {
            Logger.info("Reference node doesn't exist for factory " + this.getClass().getSimpleName()
                    + ", we create it");
            Transaction tx = graphDb.beginTx();
            try {
                Node node = graphDb.createNode();
                node.setProperty(NODE_KEY_COUNTER, new Long(1));
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
     * Method to retrieve a node by a key.
     *
     * @param key       the idenfifier of the node
     * @param indexName Name of the index on wich to search
     * @return
     */
    public Node getByKey(Long key, String indexName) {
        indexName = indexName.toUpperCase();
        Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
        Node node = indexNode.get("key", key).getSingle();
        return node;
    }

    /**
     * Method to save/update and index a node.
     *
     * @param nodeWrapper to save
     * @return the save or update node
     * @throws Neo4jException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    public Neo4jModel saveAndIndex(Neo4jModel nodeWrapper) throws Neo4jException {
        // initialisation of the method
        Transaction tx = Neo4j.db().beginTx();
        Boolean isNewNode = Boolean.FALSE;
        Map oldValues = new HashMap<String, Object>();
        if (nodeWrapper.getNode() == null) {
            isNewNode = Boolean.TRUE;
        }

        try {
            // if is a new objetc (doesn't have a node value), we create the node & generate an auto key
            if (isNewNode) {
                nodeWrapper.setKey(getNextId());
                nodeWrapper.setNode(Neo4j.db().createNode());
            }

            // setting properties node and stock oldValue into an hashmap for indexes
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {
                if (!field.getName().equals("node") && !field.getName().equals("shouldBeSave")
                        && field.get(nodeWrapper) != null) {
                    Object oldValue = nodeWrapper.getNode().getProperty(field.getName(), null);
                    if (oldValue != null) {

                    }
                    nodeWrapper.getNode().setProperty(field.getName(), field.get(nodeWrapper));
                }
            }

            // create the reference 2 node relationship
            referenceNode.createRelationshipTo(nodeWrapper.getNode(), this.ref2node);

            // create indexes ...
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {

                // create an index on the field if there is the annotaton and field value is not null
                String indexName = getIndexName(nodeWrapper.getClass().getSimpleName(), field);
                if (indexName != null && field.get(nodeWrapper) != null) {

                    // create the index
                    Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
                    // here we have to remove the index when it's an update, so we take a look at the oldValues map
                    if (oldValues.get(field.getName()) != null) {
                        indexNode.remove(nodeWrapper.getNode(), field.getName(), oldValues.get(field.getName())
                                .toString());
                    }
                    indexNode.add(nodeWrapper.getNode(), field.getName(), field.get(nodeWrapper).toString());
                }
            }

            tx.success();
        } catch (IllegalArgumentException e) {
            throw new Neo4jException(e);
        } catch (IllegalAccessException e) {
            throw new Neo4jException(e);
        } finally {
            tx.finish();
        }
        return nodeWrapper;
    }

    /**
     * Method to delete a node. If it still have a relationship (otherwise than ref2node one), this method throw a
     * runtime exception.
     *
     * @param nodeWrapper to delete
     * @return the deleted object
     * @throws Neo4jException
     */
    public Neo4jModel delete(Neo4jModel nodeWrapper) throws Neo4jException {
        return _delete(nodeWrapper, Boolean.FALSE);
    }

    /**
     * Method to delete a node, also when it still have relationship. Use this method carefully !
     *
     * @param nodeWrapper to delete
     * @return the object deleted
     * @throws Neo4jException
     */
    public Neo4jModel forceDelete(Neo4jModel nodeWrapper) throws Neo4jException {
        return _delete(nodeWrapper, Boolean.TRUE);
    }

    /**
     * General(private) method to delete a node.
     *
     * @param nodeWrapper
     * @param forceDelete if this param is set to TRUE, then all relationship will be deleted before we delete the node.
     * @return The object that have been deleted.
     * @throws Neo4jException
     */
    private Neo4jModel _delete(Neo4jModel nodeWrapper, Boolean forceDelete) throws Neo4jException {
        Transaction tx = Neo4j.db().beginTx();
        try {
            Node node = nodeWrapper.getNode();

            if (!forceDelete) {
                // delete the ref2node relationship
                for (Relationship relation : node.getRelationships(ref2node, Direction.INCOMING)) {
                    relation.delete();
                }
            } else {
                // for all other relationship if foreceDelete is set to true
                for (Relationship relation : node.getRelationships()) {
                    relation.delete();
                }
            }

            // delete entity
            node.delete();
            // delete indexes
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {
                // is there an index on the field ?
                String indexName = getIndexName(nodeWrapper.getClass().getSimpleName(), field);
                if (indexName != null) {
                    Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
                    indexNode.remove(nodeWrapper.getNode());
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
        return nodeWrapper;
    }

    /**
     * Method to get the next ID for an object.
     *
     * @return
     */
    private synchronized Long getNextId() {
        Long counter = null;
        try {
            counter = (Long) referenceNode.getProperty(NODE_KEY_COUNTER);
        } catch (NotFoundException e) {
            // Create a new counter
            counter = 0L;
        }
        Transaction tx = Neo4j.db().beginTx();
        try {
            referenceNode.setProperty(NODE_KEY_COUNTER, new Long(counter + 1));
            Logger.debug("New ID for factory " + this.getClass().getSimpleName() + " is " + counter);
            tx.success();
        } finally {
            tx.finish();
        }
        return counter;
    }

    private String getIndexName(String className, java.lang.reflect.Field field) {
        String indexName = null;
        Neo4jIndex nodeIndex = field.getAnnotation(Neo4jIndex.class);
        if (nodeIndex != null) {
            // get the name of the index
            indexName = nodeIndex.value();
            if (indexName.equals("")) {
                indexName = className + "_" + field.getName();
                indexName = indexName.toUpperCase();
            }
        }
        Logger.debug("Index name for class " + className + " and field " + field.getName() + " is " + indexName);
        return indexName;
    }
}
