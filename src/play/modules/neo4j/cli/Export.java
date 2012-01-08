package play.modules.neo4j.cli;

import java.io.File;

import org.neo4j.graphdb.Node;

import play.Play;
import play.modules.neo4j.util.Neo4j;

public class Export {

    /**
     * Export YML file method !
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // initiate play! framework
        File root = new File(System.getProperty("application.path"));
        Play.init(root, System.getProperty("play.id", ""));
        // initiate DB
        Neo4j.initialize();

        // we retrieve parameters
        String filename = "data";
        String output = "conf/";
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].startsWith("--filename=")) {
                    filename = args[i].substring(11);
                }
                if (args[i].startsWith("--output=")) {
                    output = args[i].substring(9);
                }
            }
        }
        mainWork(filename, output);

    }

    public static void mainWork(String filename, String output) throws Exception {

        for (Node node : Neo4j.db().getAllNodes()) {

        }
    }
}
