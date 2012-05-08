package models;

import java.util.Date;
import java.util.Iterator;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IteratorUtil;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.model.Neo4jModel;
import play.modules.neo4j.util.Neo4j;

public class Touite extends Neo4jModel {

    public String text;

    public Date   created;

    public User getAuthor() throws Neo4jException {
        ExecutionEngine engine = new ExecutionEngine(Neo4j.db());
        //@formatter:off
        ExecutionResult result = engine.execute("" +
                "START touite=node(" + this.node.getId() + ")" +
                "MATCH touite-[:AUTHOR]->author " +
                "RETURN author " +
                "LIMIT 1");
        //@formatter:on
        User user = null;
        Iterator<Node> column = result.columnAs("author");
        for (Node node : IteratorUtil.asIterable(column)) {
            user = User.getByNode(node);
        }
        return user;
    }

}
