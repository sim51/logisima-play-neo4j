package play.modules.neo4j.cli;

import java.io.File;

import play.Play;
import play.modules.neo4j.util.Fixtures;
import play.modules.neo4j.util.Neo4j;

public class Import {

    /**
     * Import YML file method !
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        // we initiate play! framework
        File root = new File(System.getProperty("application.path"));
        Play.init(root, System.getProperty("play.id", ""));

        // we retrieve parameters
        String filename = "data";
        String input = "conf";
        Boolean reset = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--")) {
                if (args[i].startsWith("--filename=")) {
                    filename = args[i].substring(11);
                }
                if (args[i].startsWith("--input=")) {
                    input = args[i].substring(9);
                }
                if (args[i].startsWith("--reset")) {
                    reset = true;
                }
            }
        }
        Neo4j.initialize();
        if (reset) {
            Fixtures.deleteDatabase();
        }
        Fixtures.loadYml(filename + ".yml");
        Neo4j.destroy();
    }
}
