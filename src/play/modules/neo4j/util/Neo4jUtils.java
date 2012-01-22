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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import play.Play;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.model.Neo4jFactory;

/**
 * Util class, that implements static method that help to get some infos about Node and relations.
 * 
 * @author bsimard
 * 
 */
public class Neo4jUtils {

    /**
     * Method that return the class of a node.
     * 
     * @param node
     * @return
     */
    public static Class getClassNameFromNode(Node node) {
        if (node == null) {
            return null;
        }
        for (Relationship relation : node.getRelationships(Direction.INCOMING)) {
            Node startNode = relation.getStartNode();
            if (startNode.hasProperty(Neo4jFactory.NODE_KEY_COUNTER)
                    && startNode.hasProperty(Neo4jFactory.NODE_CLASS_NAME)) {
                String className = (String) startNode.getProperty(Neo4jFactory.NODE_CLASS_NAME);
                Class clazz = Play.classes.getApplicationClass(className).javaClass;
                return clazz;
            }

        }
        return null;

    }

    /**
     * Is this field has an index annotation ?
     * 
     * @param field
     * @return <code>TRUE</code> if the field has a <code>Neo4jIndexe</code> annotation, <code>FALSE</code> otherwise.
     */
    public static boolean isIndexedField(java.lang.reflect.Field field) {
        return field.getAnnotation(Neo4jIndex.class) != null;
    }

    public static String getIndexName(String className, String indexName) {
        indexName = className + "_" + indexName;
        indexName = indexName.toUpperCase();
        return indexName;
    }

    public static String getIndexName(String className, java.lang.reflect.Field field) {
        String indexName = null;
        Neo4jIndex nodeIndex = field.getAnnotation(Neo4jIndex.class);
        if (nodeIndex != null) {
            // get the name of the index
            indexName = nodeIndex.value();
            if (indexName.equals("")) {
                indexName = field.getName();
            }
            else {
                return indexName;
            }
        }
        return getIndexName(className, indexName);
    }

}
