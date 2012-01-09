package play.modules.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import play.Logger;
import play.Play;
import play.Play.Mode;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.modules.neo4j.model.Neo4jModelEnhancer;
import play.modules.neo4j.util.Neo4j;
import play.mvc.Router;

/**
 * Neo4j Play! plugin class.
 *
 * @author bsimard
 */
public class Neo4jPlugin extends PlayPlugin {

    @Override
    public void onApplicationStart() {
        // we start the database
        Logger.info("Starting neo4j database");
        if (Neo4j.db() == null) {
            Neo4j.initialize();
            registerShutdownHook(Neo4j.db());
        }
    }

    @Override
    public void onApplicationStop() {
        // we stop the database
        Logger.info("Shutdown neo4j database");
        Neo4j.destroy();

    }

    @Override
    public void enhance(ApplicationClass appClass) throws Exception {
        // for enhance Neo4jModel class, to add getter/setter on the wrapped node
        new Neo4jModelEnhancer().enhanceThisClass(appClass);
        new play.modules.neo4j.model.Neo4jRelationshipEnhancer().enhanceThisClass(appClass);
    }

    @Override
    public void onRoutesLoaded() {
        if (Play.mode == Mode.DEV) {
            // adding some route for
            Logger.debug("adding routes for Neo4j plugin");
            Router.addRoute("GET", "/@neo4j/console", "controllers.module.neo4j.Neo4jController.console");
        }
    }

    /**
     * Registers a shutdown hook for the Neo4j instance so that it shuts down nicely when the VM exits (even if you
     * "Ctrl-C" the running example before it's completed)
     *
     * @param graphDb
     */
    private static void registerShutdownHook(final GraphDatabaseService graphDb) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                graphDb.shutdown();
            }
        });
    }

}
