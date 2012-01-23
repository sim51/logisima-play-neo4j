package models;

import java.util.List;

import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.model.Neo4jModel;

public class User extends Neo4jModel {

    public String     login;

    public String     email;
    public String     firstname;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     lastname;

    @Neo4jRelatedTo("IS_FRIEND")
    public List<User> friends;

    @Neo4jRelatedTo("IS_FAMILLY")
    public List<User> famillies;

    @Neo4jRelatedTo(value = "IS_A_COLLEAGE", lazy = true)
    public List<User> colleages;

    @Neo4jRelatedTo("IS_A_CLASSMATE")
    public List<User> classmates;

}
