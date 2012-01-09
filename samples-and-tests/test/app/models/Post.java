package models;

import play.modules.neo4j.annotation.Neo4jEntity;
import play.modules.neo4j.model.Neo4jModel;

@Neo4jEntity
public class Post extends Neo4jModel {
    public String title;
    public String content;
}
