package play.modules.neo4j.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import play.Logger;
import play.Play;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.RelatedTo;
import play.modules.neo4j.annotation.RelatedToVia;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.relationship.Neo4jRelationship;
import play.modules.neo4j.relationship.Relation;
import play.modules.neo4j.util.Neo4j;
import play.modules.neo4j.util.Neo4jFactory;

/**
 * Model class for all Neo4j node. Model are a wrapper of a node object, and all getter/setter operations are delegated
 * to the underlying node (@see Neo4jEnhancer.class).
 * 
 * @author bsimard
 */
@SuppressWarnings("unchecked")
public abstract class Neo4jModel {

    /**
     * Unique id autogenerate by the factory
     */
    @Neo4jIndex
    public Long    key;

    /**
     * Underlying node of the model.
     */
    public Node    node;

    /**
     * Boolean to know if the pojo as been changed, and so if the <code>save</code> method should be invoke.
     */
    public Boolean shouldBeSave = Boolean.FALSE;
    private String relationshipName;
    private String relationshipClass;

    /**
     * Default constructor for creation.
     */
    public Neo4jModel() {
        this.shouldBeSave = Boolean.FALSE;
        initializeRelations();
    }

    /**
     * Constructor for existing node.
     * 
     * @param node
     */
    public Neo4jModel(Node node) {
        this.node = node;
        initializeRelations();
    }

    public void initializeRelations() {
        for (java.lang.reflect.Field field : this.getClass().getFields()) {
            RelatedTo relatedTo = field.getAnnotation(RelatedTo.class);
            if (relatedTo != null) {
                try {
                    field.set(this, new Relation<Neo4jModel>(this, relatedTo));
                } catch (IllegalAccessException e) {
                    Logger.error("Erreur !" + e.getMessage());
                }
            }

            RelatedToVia relatedToVia = field.getAnnotation(RelatedToVia.class);
            if (relatedToVia != null && field.getType().toString().contains("Iterator")) {
                try {
                    String className = field.getGenericType().toString().replace("java.util.Iterator<", "")
                            .replace(">", "");
                    field.set(this, getIterator(className));
                } catch (IllegalAccessException e) {
                    Logger.error("Erreur !" + e.getMessage());
                }
            }
        }
    }

    public <T extends play.modules.neo4j.relationship.Neo4jRelationship> Iterator<T> getIterator(String className) {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        this.relationshipName = play.modules.neo4j.relationship.Neo4jRelationship.calculateRelationshipName(clazz);
        this.relationshipClass = className;

        if (getNode() == null) {
            return new NullIterator();
        }
        else {
            return new RelationshipIterator();
        }
    }

    /**
     * Save method for Neo4jModel.
     * 
     * @return
     * @throws Neo4jException
     */
    public <T extends Neo4jModel> T save() throws Neo4jException {
        this._save();
        return (T) this;
    }

    /**
     * Save method for Neo4jModel.
     * 
     * @return the saved object.
     * @throws Neo4jException
     */
    public <T extends Neo4jModel> T delete() throws Neo4jException {
        this._delete();
        return (T) this;
    }

    /**
     * Save/update and index an Neo4j node. This method is private. @see <code>save()</code> method.
     * 
     * @throws Neo4jException
     */
    private void _save() throws Neo4jException {
        Neo4jFactory factory = getFactory(this.getClass());
        Neo4jModel model = factory.saveAndIndex(this);
        this.node = model.getNode();
        this.key = model.key;
        this.shouldBeSave = Boolean.FALSE;
    }

    /**
     * Delete an Neo4j node. This method is private. @see <code>delete()</code> method.
     * 
     * @return the deleted object
     * @throws Neo4jException
     */
    private void _delete() throws Neo4jException {
        Neo4jFactory factory = getFactory(this.getClass());
        factory.forceDelete(this);
        this.node = null;
    }

    protected static void _cleanUp(String className) {
        Neo4jFactory factory = getFactory(className);
        factory.cleanUp();
    }

    protected static <T extends Neo4jModel> List<T> _findAll(String className) {
        List<T> elements = new ArrayList<T>();
        Neo4jFactory factory = getFactory(className);
        elements = (List<T>) factory.findAll();
        return elements;
    }

    /**
     * Retrieve a node by it's key.
     * 
     * @return
     * @throws Neo4jException
     */
    protected static <T extends Neo4jModel> T _getByKey(Long key, String className) throws Neo4jException {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        Neo4jFactory factory = getFactory(clazz);
        return (T) factory.getByKey(key, Neo4j.getIndexName(clazz.getSimpleName(), "key"));
    }

    /**
     * Retrieve a node from the graph
     * 
     * @param node
     * @return
     */
    public static Neo4jModel getByNode(Node node) {
        String className = Neo4j.getClassNameFromNode(node);
        if (className == null) {
            return null;
        }
        return getFactory(className).getByNode(node);
    }

    /**
     * Private method to retrieve the factory of this model class.
     * 
     * @return
     */
    protected static Neo4jFactory getFactory(Class clazz) throws Neo4jException {
        Neo4jFactory factory = null;
        try {
            factory = new Neo4jFactory(clazz);
        } catch (Exception e) {
            throw new Neo4jException(e);
        }
        return factory;
    }

    /**
     * Method to delete orphelan node ... when garbage collector is running.
     */
    protected void finalize() throws Throwable {
        if (this.node.hasRelationship() == false) {
            Transaction tx = Neo4j.db().beginTx();
            try {
                this.node.delete();
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }

    /**
     * CleanUp delete the reference node and childrens (and relationships)
     */
    public static void cleanUp() {
        throw new Neo4jPlayException("cleanUp() Must be overriden by Neo4jModelEnhancer");
    }

    /**
     * Find all nodes
     * 
     * @param <T>
     * @return
     */
    public static <T extends Neo4jModel> List<T> findAll() {
        throw new Neo4jPlayException("findAll() Must be overriden by Neo4jModelEnhancer");
    }

    /**
     * Public method to retrieve a node by its key.
     * 
     * @param key
     * @return
     * @throws Neo4jException
     */
    public static <T extends Neo4jModel> T getByKey(Long key) throws Neo4jException {
        throw new Neo4jPlayException("getByKey() Must be overriden by Neo4jModelEnhancer");
    }

    private static class NullIterator implements Iterator {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }

        @Override
        public void remove() {

        }
    }

    private class RelationshipIterator<T extends Neo4jRelationship> implements Iterator<T> {

        private final Iterator<Relationship> iterator = getNode().getRelationships(
                                                              DynamicRelationshipType.withName(getRelationshipName()),
                                                              Direction.OUTGOING).iterator();

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public T next() {
            Relationship nextNode = iterator.next();
            return T.getFromRelationship(getRelationshipClass(), nextNode);
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public String getRelationshipClass() {
        return relationshipClass;
    }

    /**
     * Getter for id.
     */
    public Long getKey() {
        if (this.node != null && this.node.getProperty("key", null) != null) {
            return (Long) this.node.getProperty("key", null);
        }
        else {
            return this.key;
        }
    }

    /**
     * Setter for id.
     * 
     * @param id
     */
    public void setKey(Long id) {
        this.key = id;
    }

    /**
     * Getter for underlying node.
     * 
     * @return
     */
    public Node getNode() {
        return node;
    }

    /**
     * Setter for node.
     * 
     * @param node
     */
    public void setNode(Node node) {
        this.node = node;
    }

    /**
     * @return the shouldBeSave
     */
    public Boolean getShouldBeSave() {
        return shouldBeSave;
    }
}
