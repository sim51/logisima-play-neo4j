package play.modules.neo4j.relationship;

import org.apache.commons.lang.NotImplementedException;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import play.Play;
import play.modules.neo4j.annotation.EndNode;
import play.modules.neo4j.annotation.Neo4jEdge;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.StartNode;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;
import play.modules.neo4j.util.Neo4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class Neo4jRelationship {
    private Neo4jModel start;
    private Neo4jModel end;
    private Relationship relationship;
    private String relationshipName;
    @Neo4jIndex
    public Long key;

    public void delete() {
        Transaction tx = Neo4j.db().beginTx();
        try {
            this.relationship.delete();
            tx.success();
        } finally {
            tx.finish();
        }

    }

    public void save() throws Neo4jException {
        Transaction tx = Neo4j.db().beginTx();


        try {
            // if is a new objetc (doesn't have a node value), we create the node & generate an auto key
            boolean isNewRelationship = true;
            if (isNewRelationship) {
                Long nextId = getNextId();
                this.setKey(nextId);

                Relationship relationshipTo = start.getNode().createRelationshipTo(end.getNode(), DynamicRelationshipType.withName(getRelationshipName(this.getClass())));
                this.setRelationship(relationshipTo);
            }


            //Add attributes
            for (java.lang.reflect.Field field : this.getClass().getFields()) {
                if (Modifier.isPublic(field.getModifiers()) &&
                        !field.getName().equals("$toString0") &&
                        !field.isAnnotationPresent(StartNode.class) &&
                        !field.isAnnotationPresent(EndNode.class)) {
                    this.getRelationship().setProperty(field.getName(), field.get(this));
                }
            }


            // create an index on the field if there is the annotaton and field value is not null
            Field field = this.getClass().getField("key");
            String indexName = Neo4j.getIndexName(this.getClass().getSimpleName(), field);
            if (indexName != null && field.get(this) != null) {
                Index<Relationship> indexNode = Neo4j.db().index().forRelationships(indexName);
                indexNode.add(this.getRelationship(), field.getName(), field.get(this).toString());
            }


            tx.success();
        } catch (Exception e) {
            throw new Neo4jException(e);
        } finally {
            tx.finish();
        }
    }

    private String getRelationshipName(Class clazz) {
        if (relationshipName == null) {
            this.relationshipName = calculateRelationshipName(clazz);
        }
        return this.relationshipName;
    }

    public static String calculateRelationshipName(Class clazz) {
        Neo4jEdge annotation = (Neo4jEdge) clazz.getAnnotation(Neo4jEdge.class);
        if (annotation != null) {
            String relationshipName = clazz.getSimpleName().toUpperCase() + "_REL";
            if (annotation.type().length() > 0) {
                relationshipName = annotation.type();
            }
            return relationshipName;
        } else {
            throw new Neo4jPlayException("Please annotate your Class (" + clazz.getSimpleName() + ") with @Neo4jEdge");
        }
    }

    protected static <T extends Neo4jRelationship> T _getByKey(Long key, String className) {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        String indexName = getIndexName(clazz);
        Index<Relationship> indexRelationship = Neo4j.db().index().forRelationships(indexName);
        Relationship relationship = indexRelationship.get("key", key).getSingle();
        return getFromRelationship(className, relationship);
    }

    public static <T extends Neo4jRelationship> T getFromRelationship(String className, Relationship relationship) {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        T instance = null;
        if (relationship != null) {
            Constructor c;
            try {
                c = clazz.getDeclaredConstructor();
                c.setAccessible(true);
                instance = (T) c.newInstance();
                instance.relationship = relationship;

                instance.setStartNode(Neo4jModel.getByNode(relationship.getStartNode()));
                instance.setEndNode(Neo4jModel.getByNode(relationship.getEndNode()));

                //Add attributes
                for (Field instanceField : clazz.getFields()) {
                    if (Modifier.isPublic(instanceField.getModifiers()) &&
                            !instanceField.getName().equals("node") &&
                            !instanceField.getName().equals("$toString0")) {
                        if (instanceField.isAnnotationPresent(StartNode.class)) {
                            instanceField.set(instance, instance.start);
                        } else if (instanceField.isAnnotationPresent(EndNode.class)) {
                            instanceField.set(instance, instance.end);
                        } else {
                            instanceField.set(instance, relationship.getProperty(instanceField.getName()));
                        }
                    }
                }

            } catch (Exception e) {
                throw new Neo4jPlayException(e);
            }
        }
        return instance;
    }

    private static String getIndexName(Class clazz) {
        Field field = null;
        try {
            field = clazz.getField("key");
        } catch (NoSuchFieldException e) {
            throw new Neo4jPlayException(e);
        }
        return Neo4j.getIndexName(clazz.getSimpleName(), field);
    }

    /**
     * Method to get the next ID for an object.
     *
     * @return
     */
    private synchronized Long getNextId() {
        Long counter = null;
        try {
            counter = (Long) Neo4j.db().getReferenceNode().getProperty(getIndexName(this.getClass()));
        } catch (NotFoundException e) {
            // Create a new counter
            counter = 1L;
        }
        Transaction tx = Neo4j.db().beginTx();
        try {
            Neo4j.db().getReferenceNode().setProperty(getIndexName(this.getClass()), new Long(counter + 1));
            tx.success();
        } finally {
            tx.finish();
        }
        return counter;
    }

    public static <T extends Neo4jRelationship> T getByKey(Long key) {
        throw new NotImplementedException("Must be overridden by enhancer");
    }

    public String toString() {
        return null;
    }

    public void setStartNode(Neo4jModel model) {
        this.start = model;
    }

    public void setEndNode(Neo4jModel model) {
        this.end = model;
    }

    private Relationship getRelationship() {
        return relationship;
    }

    private void setRelationship(Relationship relationship) {
        this.relationship = relationship;
    }

    public Long getKey() {
        return key;
    }

    private void setKey(Long key) {
        this.key = key;
    }
}
