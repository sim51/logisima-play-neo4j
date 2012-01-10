package play.modules.neo4j.cli.export;

import java.util.Iterator;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;

import play.modules.neo4j.util.Neo4jFactory;

public class YmlNode {

    public String                  id;
    private Class                  model;
    private org.neo4j.graphdb.Node dbNode;

    /**
     * Constructor.
     * 
     * @param node
     */
    public YmlNode(org.neo4j.graphdb.Node node) {
        this.dbNode = node;
        // getting model clazz
        Iterator<Relationship> iter = dbNode.getRelationships(Direction.INCOMING).iterator();
        Boolean find = Boolean.FALSE;
        while (iter.hasNext() && !find) {
            Relationship relation = iter.next();
            org.neo4j.graphdb.Node endNode = relation.getEndNode();
            if (endNode.hasProperty(Neo4jFactory.NODE_KEY_COUNTER) && endNode.hasProperty(Neo4jFactory.NODE_CLASS_NAME)) {
                // model = endNode.getProperty(Neo4jFactory.NODE_CLASS_NAME);
                find = Boolean.TRUE;
            }
        }
        // getting the key value of the object
        if (dbNode.getProperty("key", null) != null) {
            id = model.getSimpleName() + "_" + (String) dbNode.getProperty("key");
        }
    }

    /**
     * Convert <code>dbNode</code> to YML format.
     * 
     * @return an yml string that represent the <code>dbNode</code>.
     */
    public String toYml() {
        String yml = "\n" + model.getSimpleName() + "(" + id + "):\n";
        // export all atributes, except key
        for (String property : dbNode.getPropertyKeys()) {
            if (dbNode.getProperty(property, null) != null && !property.equals("key")) {
                yml += "\n " + property + ": " + dbNode.getProperty(property);
            }
        }
        yml += "\n";
        return yml;
    }
}
