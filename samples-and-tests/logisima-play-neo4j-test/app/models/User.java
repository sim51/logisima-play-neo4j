package models;

import play.modules.neo4j.annotation.Neo4jEntity;
import play.modules.neo4j.model.Neo4jModel;
import factory.UserFactory;

@Neo4jEntity(UserFactory.class)
public class User extends Neo4jModel {

    public String login;
    public String email;
    public String firstname;
    public String lastname;
    public String facebookAccount;

}
