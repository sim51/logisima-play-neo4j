package controllers;

import java.util.List;

import models.User;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        List users = User.findAll();
        render(users);
    }

    public static void user(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        render(user);
    }

    public static void editUser(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        render(user);
    }

    public static void addUser() throws Neo4jException {
        render();
    }

    public static void deleteUser(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        user.delete();
        index();
    }

}
