package models;

import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.model.Neo4jModel;

public class User extends Neo4jModel {

    public String login;

    public String email;
    public String firstname;

    @Neo4jIndex("lastname")
    public String lastname;

}
