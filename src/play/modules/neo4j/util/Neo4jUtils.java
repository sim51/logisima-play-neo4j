package play.modules.neo4j.util;

import org.neo4j.graphdb.Node;

import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.exception.Neo4jPlayException;

public class Neo4jUtils {

    public static String getClassNameFromNode(Node node) {
        if (node == null) {
            return null;
        }
        if (!node.hasProperty("clazz")) {
            throw new Neo4jPlayException("This node don't have the clazz property !");
        }
        return (String) node.getProperty("clazz");
    }

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
