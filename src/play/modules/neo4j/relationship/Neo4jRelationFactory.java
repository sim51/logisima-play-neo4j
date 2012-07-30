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
package play.modules.neo4j.relationship;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import play.Logger;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;

public class Neo4jRelationFactory {

    /**
     * Method to get items represented by field <code>field</code> that have a relationship of type
     * <code>relationName</code> with direction <code>direction</code> on the node <code>node</code>.
     * 
     * @param relationName
     * @param direction
     * @param field
     * @param node
     * @return
     */
    public static <T extends Neo4jModel> List<T> getModelsFromRelation(String relationName, String direction,
            Field field, Node node) {
        // construction of the return type
        List<T> list = new ArrayList();
        try {
            if (field.getType().isAssignableFrom(List.class)) {
                node.getRelationships(Direction.valueOf(direction), DynamicRelationshipType.withName(relationName))
                        .iterator();
                for (Relationship relation : node.getRelationships(Direction.valueOf(direction),
                        DynamicRelationshipType.withName(relationName))) {
                    Node item = null;
                    if (direction.equalsIgnoreCase("OUTGOING")) {
                        item = relation.getEndNode();
                    }
                    else {
                        item = relation.getStartNode();
                    }
                    T nodeWrapper = Neo4jModel.getByNode(item);
                    list.add(nodeWrapper);
                }
            }
            else {
                throw new Neo4jPlayException("Field with 'Neo4jRelatedTo' annotation must be a List");
            }
        } catch (Exception e) {
            throw new Neo4jPlayException(e);
        }
        return list;
    }

    /**
     * Method to get item represented by field <code>field</code> that has a relationship of type
     * <code>relationName</code> with direction <code>direction</code> on the node <code>node</code>.
     * 
     * @param relationName
     * @param direction
     * @param field
     * @param node
     * @return
     */
    public static <T extends Neo4jModel> T getModelFromUniqueRelation(String relationName, String direction,
            Field field, Node node) {
        T nodeWrapper = null;
        try {
            if (Neo4jModel.class.isAssignableFrom(field.getType())) {
                for (Relationship relation : node.getRelationships(DynamicRelationshipType.withName(relationName),
                        Direction.valueOf(direction))) {
                    if (nodeWrapper == null) {
                        Node item = null;
                        if (direction.equalsIgnoreCase("OUTGOING")) {
                            item = relation.getEndNode();
                        }
                        else {
                            item = relation.getStartNode();
                        }
                        nodeWrapper = Neo4jModel.getByNode(item);
                        Logger.debug("Loading neo4j single '" + relation.getType().name() + "-" + relation.getId()
                                + "' (" + direction + ") node for node " + node.getId());
                    }
                    else {
                        throw new Neo4jPlayException(
                                "Field "
                                        + field.getName()
                                        + " of node "
                                        + node.getId()
                                        + " that has a 'Neo4jUniqueRelation' have multiple related node ... it's incompatible !!!");
                    }
                }
            }
            else {
                throw new Neo4jPlayException("Field with 'Neo4jUniqueRelation' annotation must be a Neo4jModel");
            }
        } catch (Exception e) {
            throw new Neo4jPlayException(e);
        }
        return nodeWrapper;
    }
}
