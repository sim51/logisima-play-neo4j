/**
 * This file is part of logisima-play-neo4j.
 *
 * logisima-play-neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logisima-play-neo4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with logisima-play-neo4j. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @See https://github.com/sim51/logisima-play-neo4j
 */
package play.modules.neo4j.model;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.helpers.collection.MapUtil;

import play.Logger;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.annotation.Neo4jUniqueRelation;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.relationship.Neo4jRelationFactory;
import play.modules.neo4j.util.Neo4j;
import play.modules.neo4j.util.Neo4jUtils;

public class Neo4jFactory {

    /**
     * Reference node for all object.
     */
    private static Node             referenceNode;

    /**
     * Define the relationshipType between the root node, and the model reference node.
     */
    private static RelationshipType root2ref;

    /**
     * Define the relationshipType between the model reference node and models.
     */
    private static RelationshipType ref2node;

    /**
     * Class of the Neo4j Entity
     */
    private Class                   clazz;

    /**
     * Name of keys property on each object.
     */
    public final static String      NODE_KEY_COUNTER  = "KEY_COUNTER";
    public final static String      NODE_CLASS_NAME   = "CLASSNAME";
    private final static String     REFERENCE_KEYWORD = "_REF";

    /**
     * Constructor of the Factory.
     * 
     * @param clazz
     */
    public Neo4jFactory(Class clazz) {
        this.clazz = clazz;
        GraphDatabaseService graphDb = Neo4j.db();

        if (this.clazz != null && this.clazz.getSimpleName() != null) {
            String className = this.clazz.getSimpleName().toUpperCase();
            this.root2ref = DynamicRelationshipType.withName(className + REFERENCE_KEYWORD);
            this.ref2node = DynamicRelationshipType.withName(className);
        }
        else {
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
                node.setProperty(NODE_CLASS_NAME, clazz.getName());
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
     * @param key the idenfifier of the node
     * @param indexName Name of the index on wich to search
     * @return
     */
    public Node getByKey(Long key, String indexName) {
        Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
        Node node = indexNode.get("key", key).getSingle();
        return node;
    }

    /**
     * Retrive all node ! Be carefull there is no limitation, so if you have a million of node, this method return a
     * million of node !
     * 
     * @return
     * @throws Neo4jException
     */
    public <T extends Neo4jModel> List<T> findAll() throws Neo4jException {
        List<T> elements = new ArrayList<T>();
        Iterator<Relationship> relationships = referenceNode.getRelationships(ref2node, Direction.OUTGOING).iterator();
        while (relationships.hasNext()) {
            Relationship relationship = relationships.next();
            Node endNode = relationship.getEndNode();
            T model = (T) Neo4jModel.getByNode(endNode);
            elements.add(model);
        }
        return elements;
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
        Map<String, Object> oldValues = new HashMap<String, Object>();
        Transaction tx = Neo4j.db().beginTx();
        Boolean isNewNode = Boolean.FALSE;
        if (nodeWrapper.getNode() == null) {
            isNewNode = Boolean.TRUE;
        }

        try {
            // if it's a new object (doesn't have a node value), we create the node & generate an auto key
            if (isNewNode) {
                nodeWrapper.setKey(getNextId());
                nodeWrapper.setNode(Neo4j.db().createNode());
            }

            // setting properties node and stock oldValue into an hashmap for indexes
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {
                if (!field.getName().equals("node") && !field.getName().equals("shouldBeSave")
                        && !field.getName().equals("$toString0")) {

                    // it's a classic attribute
                    if (!field.isAnnotationPresent(Neo4jRelatedTo.class)
                            && !field.isAnnotationPresent(Neo4jUniqueRelation.class)) {
                        Object oldValue = nodeWrapper.getNode().getProperty(field.getName(), null);
                        if (oldValue != null) {
                            oldValues.put(field.getName(), oldValue);
                        }
                        if (field.get(nodeWrapper) != null) {
                            nodeWrapper.getNode().setProperty(
                                    field.getName(),
                                    play.modules.neo4j.util.Binder.bindToNeo4jFormat(field.get(nodeWrapper),
                                            field.getType()));
                        }
                        else {
                            nodeWrapper.getNode().removeProperty(field.getName());
                        }
                    }
                    // it's a relation
                    else {
                        // RelatedTo annotation
                        if (field.isAnnotationPresent(Neo4jRelatedTo.class) && field.get(nodeWrapper) != null) {
                            // we retrive annotation value
                            Neo4jRelatedTo neo4jRelatedTo = field.getAnnotation(Neo4jRelatedTo.class);
                            Direction relationDirection = Direction.valueOf(neo4jRelatedTo.direction());
                            RelationshipType relationType = DynamicRelationshipType.withName(neo4jRelatedTo.value());

                            // construct an hasmap of database relation from node with begin node / relation format.
                            Map<String, Relationship> dbMapRelations = new HashMap<String, Relationship>();
                            Iterable<Relationship> dbNodeRlation = nodeWrapper.getNode().getRelationships(
                                    relationDirection, relationType);
                            for (Relationship relation : dbNodeRlation) {
                                dbMapRelations.put(relation.getStartNode().getId() + "@"
                                        + relation.getEndNode().getId(), relation);
                            }
                            // this map is the stack where relation are store and remove to khnow wich are to add or
                            // deleted
                            // !
                            Map<String, Relationship> dbMapRelationsStack = new TreeMap(dbMapRelations);

                            // for all node in this node relation, we look if it is in the map
                            List<Neo4jModel> relations = (List) field.get(nodeWrapper);
                            for (Neo4jModel related : relations) {
                                if (related.shouldBeSave) {
                                    throw new Neo4jPlayException(
                                            "You have to 'save' all related model, before to 'save' parent model");
                                }
                                // looking for start node (that's why Neo4jRelatedTo can't have "BOTH" value for
                                // direction).
                                Node startNode;
                                Node endNode;
                                if (relationDirection.equals(Direction.INCOMING)) {
                                    startNode = related.node;
                                    endNode = nodeWrapper.getNode();
                                }
                                else {
                                    startNode = nodeWrapper.getNode();
                                    endNode = related.node;
                                }

                                // if dbMap has startNode, then it's OK, nothing to do
                                if (dbMapRelationsStack.containsKey(startNode.getId() + "@" + endNode.getId())) {
                                    dbMapRelationsStack.remove(startNode.getId() + "@" + endNode.getId());
                                }
                                // startNode is not in dbMap so we add it !
                                else {
                                    // Here we do a test if this relation is already in database (this due to the list
                                    // where
                                    // we can add the same object more than one times ...)
                                    if (!dbMapRelations.containsKey(startNode.getId() + "@" + endNode.getId())) {
                                        startNode.createRelationshipTo(endNode, relationType);
                                    }
                                }
                            }

                            // if dbMap still contain data, we have to deleted some relations !
                            if (dbMapRelationsStack.size() != 0) {
                                for (Relationship relation : dbMapRelationsStack.values()) {
                                    relation.delete();
                                }
                            }
                        }

                        // UniqueRelation annotation
                        if (field.isAnnotationPresent(Neo4jUniqueRelation.class)) {
                            // we retrive annotation value
                            Neo4jUniqueRelation neo4jUnique = field.getAnnotation(Neo4jUniqueRelation.class);
                            Direction relationDirection = Direction.valueOf(neo4jUnique.direction());
                            RelationshipType relationType = DynamicRelationshipType.withName(neo4jUnique.value());
                            // get GETTER method
                            String propertyName = field.getName().substring(0, 1).toUpperCase()
                                    + field.getName().substring(1);
                            String getterName = "get" + propertyName;
                            Method getter = nodeWrapper.getClass().getMethod(getterName);
                            // the current value of the field
                            Neo4jModel current = (Neo4jModel) getter.invoke(nodeWrapper);
                            // the previous value of the field
                            Neo4jModel previous = Neo4jRelationFactory.getModelFromUniqueRelation(neo4jUnique.value(),
                                    neo4jUnique.direction(), field, nodeWrapper.node);

                            if (current != null && current.shouldBeSave) {
                                throw new Neo4jPlayException(
                                        "You have to 'save' all related model, before to 'save' parent model");
                            }

                            // first we look if there is a change !
                            if ((current != null && previous != null && current.node.getId() != previous.node.getId())
                                    | !(current == null && previous == null)) {
                                Node startNode;
                                Node endNode;
                                // if previous is null : simple add
                                if (previous == null) {
                                    if (relationDirection.equals(Direction.INCOMING)) {
                                        createRelationship(current.node, nodeWrapper.getNode(), relationType);
                                    }
                                    else {
                                        createRelationship(nodeWrapper.getNode(), current.node, relationType);
                                    }
                                }
                                else {
                                    // if current is null => deletion
                                    if (current == null) {
                                        if (neo4jUnique.line()) {
                                            throw new Neo4jException(
                                                    "You can't have a null value when line mode is activated. If you want to delete the chain, you have to do it item by item !");
                                        }
                                        else {
                                            Relationship relation = nodeWrapper.node.getSingleRelationship(
                                                    relationType, relationDirection);
                                            relation.delete();
                                            // TODO : Delete node if there is no relation ?
                                            // previous.node.delete();
                                        }
                                    }
                                    // else
                                    else {
                                        if (neo4jUnique.line()) {
                                            if (relationDirection.equals(Direction.INCOMING)) {
                                                Relationship relation = nodeWrapper.getNode().getSingleRelationship(
                                                        relationType, relationDirection);
                                                relation.delete();
                                                createRelationship(current.getNode(), nodeWrapper.getNode(),
                                                        relationType);
                                                RelationshipType nextRelationType = DynamicRelationshipType
                                                        .withName(neo4jUnique.value() + "_NEXT");
                                                createRelationship(previous.getNode(), current.getNode(),
                                                        nextRelationType);

                                                createRelationship(current.getNode(), nodeWrapper.getNode(),
                                                        relationType);
                                            }
                                            else {
                                                Relationship relation = nodeWrapper.getNode().getSingleRelationship(
                                                        relationType, relationDirection);
                                                relation.delete();
                                                createRelationship(nodeWrapper.getNode(), current.getNode(),
                                                        relationType);
                                                RelationshipType nextRelationType = DynamicRelationshipType
                                                        .withName(neo4jUnique.value() + "_NEXT");
                                                createRelationship(current.getNode(), previous.getNode(),
                                                        nextRelationType);
                                            }
                                        }
                                        else {
                                            Relationship relation = nodeWrapper.getNode().getSingleRelationship(
                                                    relationType, relationDirection);
                                            relation.delete();
                                            // TODO : Delete node if there is no relation ?
                                            // previous.node.delete();
                                            if (relationDirection.equals(Direction.INCOMING)) {
                                                createRelationship(current.getNode(), nodeWrapper.getNode(),
                                                        relationType);
                                            }
                                            else {
                                                createRelationship(nodeWrapper.getNode(), current.getNode(),
                                                        relationType);
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isNewNode) {
                // create the reference 2 node relationship
                referenceNode.createRelationshipTo(nodeWrapper.getNode(), this.ref2node);
            }

            // create indexes ...
            for (java.lang.reflect.Field field : nodeWrapper.getClass().getFields()) {
                // create an index on the field if there is the annotaton and field value is not null
                if (Neo4jUtils.isIndexedField(field)) {
                    indexNodeField(nodeWrapper, field, oldValues.get(field.getName()));
                }
            }

            tx.success();
        } catch (Exception e) {
            throw new Neo4jException(e);
        } finally {
            tx.finish();
        }
        return nodeWrapper;
    }

    /**
     * Private method that is use into saveAndIndex method. It create or update the index of a field.
     * 
     * @param nodeWrapper
     * @param field
     * @param oldValue
     * @throws IllegalAccessException
     */
    private void indexNodeField(Neo4jModel nodeWrapper, Field field, Object oldValue) throws IllegalAccessException {
        String indexName = Neo4jUtils.getIndexName(nodeWrapper.getClass().getSimpleName(), field);
        if (indexName != null && field.get(nodeWrapper) != null) {
            // create the index
            Index<Node> indexNode = Neo4j.db().index().forNodes(indexName, getLuceneConfigParam(field));
            // here we have to remove the index when it's an update, so we take a look at the oldValues map
            if (oldValue != null) {
                indexNode.remove(nodeWrapper.getNode(), field.getName(), oldValue.toString());
            }
            indexNode.add(nodeWrapper.getNode(), field.getName(), field.get(nodeWrapper).toString());
        }

    }

    /**
     * Construct lucene params configuration by the Neo4jIndex annotation.
     * 
     * @param field
     * @return
     */
    private Map<String, String> getLuceneConfigParam(Field field) {
        Neo4jIndex nodeIndex = field.getAnnotation(Neo4jIndex.class);
        Map<String, String> params = new HashMap<String, String>();
        params.put(IndexManager.PROVIDER, "lucene");
        params.put("type", nodeIndex.type());
        if (nodeIndex.type().equals("fulltext") && nodeIndex.lowerCase() != "") {
            return MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", nodeIndex.type(), "to_lower_case",
                    nodeIndex.lowerCase());
        }
        else {
            return MapUtil.stringMap(IndexManager.PROVIDER, "lucene", "type", nodeIndex.type());
        }
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
        if (nodeWrapper.getNode() == null) {
            return null;
        }
        Transaction tx = Neo4j.db().beginTx();
        try {
            Node node = nodeWrapper.getNode();

            if (!forceDelete) {
                // delete the ref2node relationship
                for (Relationship relation : node.getRelationships(ref2node, Direction.INCOMING)) {
                    relation.delete();
                }
            }
            else {
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
                if (Neo4jUtils.isIndexedField(field)) {
                    String indexName = Neo4jUtils.getIndexName(nodeWrapper.getClass().getSimpleName(), field);
                    if (indexName != null) {
                        Index<Node> indexNode = Neo4j.db().index().forNodes(indexName);
                        indexNode.remove(nodeWrapper.getNode());
                    }
                }
            }
            tx.success();
        } catch (Exception e) {
            throw new Neo4jPlayException(e);
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

    private void createRelationship(Node start, Node end, RelationshipType relationType) {
        start.createRelationshipTo(end, relationType);
    }

}
