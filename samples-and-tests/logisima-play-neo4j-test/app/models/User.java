package models;

import java.util.Date;
import java.util.List;

import org.neo4j.graphdb.Direction;

import play.db.jpa.Blob;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.annotation.Neo4jUniqueRelation;
import play.modules.neo4j.model.Neo4jModel;

import com.google.gson.Gson;

public class User extends Neo4jModel {

    public String     login;

    public String     email;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     firstname;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     lastname;

    public Integer    age;

    public Long       devScore;

    public Date       birthday;

    public Blob       avatar;

    public Boolean    isActive;

    @Neo4jRelatedTo(value = "IS_FRIEND", lazy = true)
    public List<User> friends;

    @Neo4jRelatedTo(value = "IS_FRIEND", direction = Direction.INCOMING, lazy = true)
    public List<User> reversefriends;

    @Neo4jRelatedTo(value = "IS_FRIEND", direction = Direction.BOTH, lazy = true)
    public List<User> allFriends;

    @Neo4jRelatedTo("IS_FAMILLY")
    public List<User> famillies;

    @Neo4jRelatedTo(value = "IS_A_COLLEAGE")
    public List<User> colleages;

    @Neo4jRelatedTo(value = "IS_A_CLASSMATE")
    public List<User> classmates;

    @Neo4jUniqueRelation(value = "NEXT_JOB", line = true)
    public Job        job;

    @Neo4jUniqueRelation(value = "ADDRESS", line = false)
    public Address    address;

    public String toString() {
        return new Gson().toJson(this);
    }

}
