package models;

import play.modules.neo4j.annotation.Neo4jEdge;
import play.modules.neo4j.annotation.Neo4jEndNode;
import play.modules.neo4j.annotation.Neo4jStartNode;
import play.modules.neo4j.relationship.Neo4jRelationship;

@Neo4jEdge(type = "BOOKMARKED")
public class Bookmark extends Neo4jRelationship {

    @Neo4jStartNode
    public User user;

    @Neo4jEndNode
    public Post post;

    public int  stars;

}
