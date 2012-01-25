package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.User;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;

import play.db.jpa.Blob;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Neo4j;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        List users = User.findAll();
        render(users);
    }

    public static void user(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        Date date = user.birthday;
        Blob blob = user.avatar;
        render(user);
    }

    public static void editUser(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        render(user);
    }

    public static void addUser() throws Neo4jException {
        render("Application/editUser.html");
    }

    public static void saveUser(User user) throws Neo4jException {
        user.save();
        user(user.key);
    }

    public static void deleteUser(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        user.delete();
        index();
    }

    public static void searchUser(String query) {
        List<User> users = new ArrayList<User>();
        IndexManager index = Neo4j.db().index();
        Index<Node> usersIndex = index.forNodes("lastname");
        for (Node node : usersIndex.query("lastname", query)) {
            User user = new User();
            user.setNode(node);
            users.add(user);
        }
        render(users);
    }

    public static void userAvatar(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        response.setContentTypeIfNotSet(user.avatar.type());
        renderBinary(user.avatar.getFile());
    }

}
