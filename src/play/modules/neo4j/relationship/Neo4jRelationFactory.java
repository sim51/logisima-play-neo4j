package play.modules.neo4j.relationship;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;

public class Neo4jRelationFactory {

    public static <T extends Neo4jModel> List<T> getModelsFromRelatedTo(String relationName, String direction,
            Field field, Node node) {
        // construction of the return type
        List<T> list = new ArrayList();
        try {
            if (field.getType().isAssignableFrom(List.class)) {
                node.getRelationships(Direction.valueOf(direction), DynamicRelationshipType.withName(relationName))
                        .iterator();
                for (Relationship relation : node.getRelationships(Direction.valueOf(direction),
                        DynamicRelationshipType.withName(relationName))) {
                    Node item = relation.getEndNode();
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
}
