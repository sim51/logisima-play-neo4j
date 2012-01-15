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
