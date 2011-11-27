package play.modules.neo4j.model;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import play.modules.neo4j.annotation.Neo4jEntity;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.util.AbstractNeo4jFactory;
import play.modules.neo4j.util.Neo4j;

/**
 * Model class for all Neo4j node. Model are a wrapper of a node object, and all getter/setter operations are delegated
 * to the underlying node (@see Neo4jEnhancer.class).
 * 
 * @author bsimard
 * 
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
     * Save/update and index an Neo4j node. This method is private. @see <code>save()</code> method.
     * 
     * @return
     * @throws Neo4jException
     */
    private void _save() throws Neo4jException {
        AbstractNeo4jFactory factory = getFactory(this.getClass());
        factory.saveAndIndex(this);
        this.shouldBeSave = Boolean.FALSE;
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
     * Retrieve a node by it's key.
     * 
     * @return
     * @throws Neo4jException
     */
    protected static <T extends Neo4jModel> T getByKey(Long key, String className) throws Neo4jException {
        Neo4jModel nodeWrapper = null;
        try {
            Class clazz = Class.forName(className);
            AbstractNeo4jFactory factory = getFactory(clazz);
            String indexName = clazz.getSimpleName() + "_KEY";
            Node node = factory.getByKey(key, indexName);

            Constructor c;
            c = clazz.getDeclaredConstructor();
            c.setAccessible(true);
            nodeWrapper = (Neo4jModel) c.newInstance();
            nodeWrapper.setNode(node);
        } catch (Exception e) {
            throw new Neo4jPlayException("Error when create a Neo4jModel by reflection :" + e.getCause());
        }
        return (T) nodeWrapper;
    }

    /**
     * Private method to retrieve the factory of this model class.
     * 
     * @return
     * @throws Neo4jException
     */
    protected static AbstractNeo4jFactory getFactory(Class clazz) throws Neo4jException {
        AbstractNeo4jFactory factory = null;
        Neo4jEntity entity = (Neo4jEntity) clazz.getAnnotation(Neo4jEntity.class);
        if (entity != null) {
            try {
                Class<?> factoryClass = entity.value();
                Constructor ct;
                ct = factoryClass.getConstructor();
                factory = (AbstractNeo4jFactory) ct.newInstance();
            } catch (Exception e) {
                throw new Neo4jException(e);
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
