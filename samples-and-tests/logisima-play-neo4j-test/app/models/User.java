package models;

import java.util.Date;
import java.util.List;

import play.db.jpa.Blob;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.model.Neo4jModel;

public class User extends Neo4jModel {

    public String     login;

    public String     email;

    public String     firstname;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     lastname;

    public Integer    age;

    public Long       devScore;

    public Date       birthday;

    public Blob       avatar;

    public Boolean    isActive;

    @Neo4jRelatedTo("IS_FRIEND")
    public List<User> friends;

    @Neo4jRelatedTo("IS_FAMILLY")
    public List<User> famillies;

    @Neo4jRelatedTo(value = "IS_A_COLLEAGE")
    public List<User> colleages;

    @Neo4jRelatedTo(value = "IS_A_CLASSMATE", lazy = true)
    public List<User> classmates;

}
