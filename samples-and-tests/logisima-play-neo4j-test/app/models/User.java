package models;

import java.util.List;

import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.model.Neo4jModel;

public class User extends Neo4jModel {

    public String     login;

    public String     email;
    public String     firstname;

    @Neo4jIndex("lastname")
    public String     lastname;

    @Neo4jRelatedTo("IS_FRIEND")
    public List<User> friends;

    @Neo4jRelatedTo("IS_FAMILLY")
    public List<User> famillies;

    @Neo4jRelatedTo("IS_COLLEAGE")
    public List<User> colleages;

    @Neo4jRelatedTo("IS_CLASSMATE")
    public List<User> classmates;

}
