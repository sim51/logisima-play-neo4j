package models;

import play.modules.neo4j.annotation.EndNode;
import play.modules.neo4j.annotation.Neo4jEdge;
import play.modules.neo4j.annotation.StartNode;
import play.modules.neo4j.model.Neo4jRelationship;

@Neo4jEdge(type = "BOOKMARKED")
public class Bookmark extends Neo4jRelationship {
    @StartNode
    public User user;

    @EndNode
    public Post post;

    public int stars;

}
