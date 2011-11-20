package play.modules.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import play.Logger;
import play.Play;

/**
 * Class to retrieve a valid <code>GraphDatabaseService</code> for application (<code>graphDb</code> is in a
 * <code>ThreadLocal</code>).
 * 
 * @author bsimard
 * 
 */
public class Neo4j {

    private static ThreadLocal<GraphDatabaseService> graphDb = new ThreadLocal<GraphDatabaseService>();

    /**
     * Method to create graphDb instance (start the server).
     * 
     * @throws Neo4jException
     */
    public static void initialize() throws Neo4jException {
        if (graphDb.get() != null) {
            throw new Neo4jException("The graphDb is already initialize.");
        }
        String DBPath = Play.configuration.getProperty("neo4j.path");
        Logger.debug("Neo4j database path is :" + DBPath);
        EmbeddedGraphDatabase graph = new EmbeddedGraphDatabase(DBPath);
        graphDb.set(graph);
    }

    /**
     * Methode to destroy all reference.
     */
    public static void destroy() {
        if (graphDb.get() != null) {
            db().shutdown();
            graphDb.remove();
        }
    }

    /**
     * Method to retrieve the graphDb into the ThreadLocal.
     * 
     * @return
     */
    public static GraphDatabaseService db() {
        return graphDb.get();
    }

}
