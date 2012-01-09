package models;

import play.modules.neo4j.annotation.Neo4jEntity;
import play.modules.neo4j.annotation.RelatedTo;
import play.modules.neo4j.annotation.RelatedToVia;
import play.modules.neo4j.model.Neo4jModel;

import java.util.Iterator;
import java.util.Set;

@Neo4jEntity
public class User extends Neo4jModel {
    public String login;

    public String email;

    @RelatedTo(type = "KNOWS")
    public Set<User> friends;

    @RelatedToVia
    public Iterator<Bookmark> bookmarks;

    @Override
    public String toString() {
        return "User{" + getKey() + ", " +
                "login='" + login + '\'' +
                ", email='" + email + '\'' +
                ", friends=" + friends +
                '}';
    }
}
