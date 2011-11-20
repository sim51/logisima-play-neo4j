package play.modules.neo4j.model;

import java.lang.reflect.Constructor;

import org.neo4j.graphdb.Node;

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
    public Long        id;

    /**
     * underlying node of the model.
     */
    private final Node underlyingNode;

    /**
     * Default constructor for creation.
     */
    public Neo4jModel() {
        this.underlyingNode = Neo4j.db().createNode();
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
    public Long getId() {
        return (Long) this.underlyingNode.getProperty("id");
    }

    /**
     * Setter for id.
     * 
     * @param id
     */
    public void setId(Long id) {
        this.underlyingNode.setProperty("id", id);
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
     * Save/update and index an Neo4j node.
     * 
     * @return
     * @throws Neo4jReflectionException
     */
    public Neo4jModel save() throws Neo4jReflectionException {
        Neo4jEntity entity = this.getClass().getAnnotation(Neo4jEntity.class);
        if (entity != null) {
            try {
                Class<?> factoryClass = entity.value();
                Constructor ct;
                ct = factoryClass.getConstructor();
                AbstractNeo4jFactory factory = (AbstractNeo4jFactory) ct.newInstance();
                factory.saveAndIndex(this);
            } catch (Exception e) {
                throw new Neo4jReflectionException(e);
            }
        }
        return null;
    }
}
