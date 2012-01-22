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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import play.Play;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.annotation.Neo4jRelatedToVia;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.relationship.Neo4jRelationship;
import play.modules.neo4j.relationship.Relation;
import play.modules.neo4j.util.Neo4j;
import play.modules.neo4j.util.Neo4jUtils;

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

    /**
     * 
     */
    private String relationshipName;

    /**
     * 
     */
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

    /**
     * Initialize relation for model.
     */
    private void initializeRelations() {
        // for all field, we look at it to see if there are some Related annotation
        for (java.lang.reflect.Field field : this.getClass().getFields()) {
            Neo4jRelatedTo relatedTo = field.getAnnotation(Neo4jRelatedTo.class);
            if (relatedTo != null) {
                try {
                    // we set the default property with a new Relation ship Object
                    field.set(this, new Relation<Neo4jModel>(this, relatedTo));
                } catch (IllegalAccessException e) {
                    throw new Neo4jPlayException(e);
                }
            }

            Neo4jRelatedToVia relatedToVia = field.getAnnotation(Neo4jRelatedToVia.class);
            if (relatedToVia != null && field.getType().toString().contains("Iterator")) {
                try {
                    // we set the default property with a new Relation ship Object
                    String className = field.getGenericType().toString().replace("java.util.Iterator<", "")
                            .replace(">", "");
                    field.set(this, getIterator(className));
                } catch (IllegalAccessException e) {
                    throw new Neo4jPlayException(e);
                }
            }
        }
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

    /**
     * Getter for relationshipName.
     * 
     * @return
     */
    public String getRelationshipName() {
        return relationshipName;
    }

    /**
     * Getter for relationshipclass
     * 
     * @return
     */
    public String getRelationshipClass() {
        return relationshipClass;
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

    /**
     * FindAll method for Neo4jModel. Becarefull there is no limitation. So if you have a millon of node, this metod
     * return a million of item ...
     * 
     * @return
     */
    public static <T extends Neo4jModel> List<T> findAll() {
        throw new Neo4jPlayException("findAll() Must be overriden by Neo4jModelEnhancer");
    }

    /**
     * Find all nodes. Becarefull there is no limitation. So if you have a millon of node, this metod return a million
     * of item ...
     * 
     * @param <T>
     * @return
     */
    protected static <T extends Neo4jModel> List<T> _findAll(String className) throws Neo4jException {
        List<T> elements = new ArrayList<T>();
        Neo4jFactory factory = getFactory(className);
        elements = (List<T>) factory.findAll();
        return elements;
    }

    /**
     * Method to retrieve a node by its key.
     * 
     * @param key
     * @return
     * @throws Neo4jException
     */
    public static <T extends Neo4jModel> T getByKey(Long key) throws Neo4jException {
        throw new Neo4jPlayException("getByKey() Must be overriden by Neo4jModelEnhancer");
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
        return (T) factory.getByKey(key, Neo4jUtils.getIndexName(clazz.getSimpleName(), "key"));
    }

    /**
     * Retrieve a Neo4jModel from a node.
     * 
     * @param node
     * @return
     * @throws Neo4jException
     */
    public static Neo4jModel getByNode(Node node) throws Neo4jException {
        Class clazz = Neo4jUtils.getClassNameFromNode(node);
        Neo4jModel nodeWrapper = null;
        try {
            Constructor c;
            c = clazz.getDeclaredConstructor(Node.class);
            c.setAccessible(true);
            nodeWrapper = (Neo4jModel) c.newInstance(node);
        } catch (Exception e) {
            throw new Neo4jException(e);
        }
        return nodeWrapper;
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
     * Private method to retrieve the factory of this model class.
     * 
     * @return
     * @throws Neo4jException
     */
    private static Neo4jFactory getFactory(String className) throws Neo4jException {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        return getFactory(clazz);
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
     * ?????
     */
    public <T extends Neo4jRelationship> Iterator<T> getIterator(String className) {
        Class clazz = Play.classes.getApplicationClass(className).javaClass;
        this.relationshipName = Neo4jRelationship.calculateRelationshipName(clazz);
        this.relationshipClass = className;

        if (getNode() == null) {
            return new NullIterator();
        }
        else {
            return new RelationshipIterator();
        }
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
}
