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
package play.modules.neo4j.cli.export;

import org.neo4j.graphdb.Relationship;

public class YmlRelation {

    public String   id;
    private YmlNode startNode;
    private YmlNode endNode;
    private String  relationName;

    /**
     * Constructor.
     */
    public YmlRelation(Relationship relation) {
        this.id = "" + relation.getId();
        this.startNode = new YmlNode(relation.getStartNode());
        this.endNode = new YmlNode(relation.getEndNode());
        this.relationName = relation.getType().name();
    }

    /**
     * Convert <code>Relation</code> to YML format.
     * 
     * @return an yml string that represent the <code>Relation</code>.
     */
    public String toYml() {
        System.out.println("Generate yml for relation " + id);
        String yml = "\nRelation(" + this.id + "):";
        yml += "\n type: " + relationName;
        yml += "\n from: " + startNode.id;
        yml += "\n to: " + endNode.id;
        yml += "\n";
        return yml;
    }

}
