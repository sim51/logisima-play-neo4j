package play.modules.neo4j;

import org.neo4j.graphdb.Node;

/**
 * Model class for all Neo4j node. Model are a wrapper of a node object, and all getter/setter operations are delegated
 * to the underlying node (@see Neo4jEnhancer.class).
 * 
 * @author bsimard
 * 
 */
public abstract class Neo4jModel {

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

}
