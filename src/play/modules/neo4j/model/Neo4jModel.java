package play.modules.neo4j.model;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import play.modules.neo4j.annotation.Neo4jEntity;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.exception.Neo4jReflectionException;
import play.modules.neo4j.util.AbstractNeo4jFactory;
import play.modules.neo4j.util.Neo4j;

/**
 * Model class for all Neo4j node. Model are a wrapper of a node object, and all getter/setter operations are delegated
 * to the underlying node (@see Neo4jEnhancer.class).
 * 
 * @author bsimard
 * 
 */
public abstract class Neo4jModel {

    /**
     * Unique id autogenerate by the factory
     */
    @Neo4jIndex
    public Long    key;

    /**
     * Underlying node of the model.
     */
    public Node    underlyingNode;

    /**
     * Boolean to know if the pojo as been changed, and so if the <code>save</code> method should be invoke.
     */
    public Boolean shouldBeSave = Boolean.FALSE;

    /**
     * Default constructor for creation.
     */
    public Neo4jModel() {
        this.shouldBeSave = Boolean.TRUE;
    }

    /**
     * Constructor for existing node.
     * 
     * @param underlyingNode
     */
    public Neo4jModel(Node underlyingNode) {
        this.underlyingNode = underlyingNode;
    }

    /**
     * Getter for id.
     */
    public Long getKey() {
        return (Long) this.underlyingNode.getProperty("key", null);
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
        return underlyingNode;
    }

    /**
     * Setter for underlying node.
     * 
     * @param underlyingNode
     */
    public void setNode(Node underlyingNode) {
        this.underlyingNode = underlyingNode;
    }

    /**
     * @return the shouldBeSave
     */
    public Boolean getShouldBeSave() {
        return shouldBeSave;
    }

    /**
     * Save/update and index an Neo4j node. This method is private. @see <code>save()</code> method into enhancer.
     * 
     * @return
     * @throws Neo4jReflectionException
     */
    private Neo4jModel save() throws Neo4jReflectionException {
        AbstractNeo4jFactory factory = getFactory();
        factory.saveAndIndex(this);
        return this;
    }

    /**
     * Private method to retrieve the factory of this model class.
     * 
     * @return
     * @throws Neo4jReflectionException
     */
    private AbstractNeo4jFactory getFactory() throws Neo4jReflectionException {
        AbstractNeo4jFactory factory = null;
        Neo4jEntity entity = this.getClass().getAnnotation(Neo4jEntity.class);
        if (entity != null) {
            try {
                Class<?> factoryClass = entity.value();
                Constructor ct;
                ct = factoryClass.getConstructor();
                factory = (AbstractNeo4jFactory) ct.newInstance();
            } catch (Exception e) {
                throw new Neo4jReflectionException(e);
            }
        }
        return factory;
    }

    /**
     * Method to delete orphelan node ... when garbage collector is running.
     */
    protected void finalize() throws Throwable {
        if (this.underlyingNode.hasRelationship() == false) {
            Transaction tx = Neo4j.db().beginTx();
            try {
                this.underlyingNode.delete();
                tx.success();
            } finally {
                tx.finish();
            }
        }
    }
}
