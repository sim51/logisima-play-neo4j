package controllers;

import models.User;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;

public class Register extends Controller {

    public static void register(User user) throws Neo4jException {
        render("Application/editUser.html");
    }

    public static void saveUser(User user) throws Neo4jException {
        user.save();
        Application.index();
    }
}
