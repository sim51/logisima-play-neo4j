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
package play.modules.neo4j.util;

import org.neo4j.gis.spatial.SpatialDatabaseService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.server.WrappingNeoServerBootstrapper;

import play.Logger;
import play.Play;
import play.Play.Mode;
import play.modules.neo4j.exception.Neo4jPlayException;

/**
 * Class to retrieve a valid <code>GraphDatabaseService</code> for application (<code>graphDb</code> is in a
 * <code>ThreadLocal</code>).
 * 
 * @author bsimard
 */
public class Neo4j {

    private static volatile GraphDatabaseService          graphDb;
    private static volatile WrappingNeoServerBootstrapper bootstrapperDb;
    private static volatile SpatialDatabaseService        spatialDb;

    /**
     * Method to create graphDb instance (start the server).
     * 
     * @throws Neo4jPlayException
     */
    public static void initialize() throws Neo4jPlayException {
        if (graphDb != null) {
            throw new Neo4jPlayException("The graphDb is already initialize.");
        }
        String DBPath = Play.configuration.getProperty("neo4j.path");
        Logger.debug("Neo4j database path is :" + DBPath);
        EmbeddedGraphDatabase graph = new EmbeddedGraphDatabase(DBPath);
        graphDb = graph;
        if (Play.mode == Mode.DEV) {
            WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(graph);
            bootstrapper.start();
            bootstrapperDb = bootstrapper;
        }
        Boolean isSpatial = Boolean.valueOf(Play.configuration.getProperty("neo4j.spatial", "false"));
        if (isSpatial) {
            SpatialDatabaseService spatial = new SpatialDatabaseService(graph);
            spatialDb = spatial;
        }
    }

    /**
     * Methode to destroy all reference.
     */
    public static void destroy() {
        if (Play.mode == Mode.DEV) {
            if (bootstrapperDb != null) {
                bootstrapperDb.stop();
            }
        }
        if (graphDb != null) {
            graphDb.shutdown();
        }
    }

    /**
     * Method to retrieve the graphDb into the ThreadLocal.
     * 
     * @return
     */
    public static GraphDatabaseService db() {
        if (graphDb == null) {
            Logger.debug("Can't get DB from local thread !!!");
        }
        Logger.debug("Current thread is " + Thread.currentThread().getName() + " " + Thread.currentThread().getId());
        return graphDb;
    }

    /**
     * Method to retrieve the spatialDb into the ThreadLocal.
     * 
     * @return
     */
    public static SpatialDatabaseService spatial() {
        return spatialDb;
    }

    /**
     * Method to reinitialize the graph database.
     */
    public static void clear() {
        Transaction tx = Neo4j.db().beginTx();
        try {
            // for all node, we first delete all relation, and after we delete the node
            for (Node node : db().getAllNodes()) {
                Logger.debug("Deleting node " + node.getId());
                for (Relationship relation : node.getRelationships()) {
                    Logger.debug("Deleting relation " + relation.getId() + " for node " + node.getId());
                    relation.delete();
                }
                // if node is the reference, we doesn't delete it, but we reset its properties
                if (node.getGraphDatabase().getReferenceNode().equals(node)) {
                    for (String property : node.getPropertyKeys()) {
                        node.removeProperty(property);
                    }
                }
                else {
                    node.delete();
                }
            }

            // Deleting indexes
            String[] nodeIndexNames = Neo4j.db().index().nodeIndexNames();
            for (int i = 0; i < nodeIndexNames.length; i++) {
                Logger.debug("Deleting node index  " + nodeIndexNames[i]);
                Neo4j.db().index().forNodes(nodeIndexNames[i]).delete();
            }
            String[] relationIdexNames = Neo4j.db().index().relationshipIndexNames();
            for (int j = 0; j < relationIdexNames.length; j++) {
                Logger.debug("Deleting relation index  " + nodeIndexNames[j]);
                Neo4j.db().index().forNodes(relationIdexNames[j]).delete();
            }
            tx.success();
            Logger.debug("Deletion commiting");
        } finally {
            tx.finish();
        }

    }

}
