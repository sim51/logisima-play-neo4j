package controllers;

import models.User;
import play.Logger;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        render();
    }

    public static void add() {
        User user = new User();
        user.email = "bsimard@logisima.com";
        user.firstname = "Benoit";
        user.lastname = "SIMARD";
        user.login = "bsimard";
        try {
            user.save();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage(), e);
        }
        index();
    }

    public static void getByKey() {
        User user = new User();
        user.email = "bsimard@logisima.com";
        user.firstname = "Benoit";
        user.lastname = "SIMARD";
        user.login = "bsimard";
        try {
            user.save();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage(), e);
        }
        render();
    }

    public static void update() {
        User user = new User();
        user.email = "bsimard@logisima.com";
        user.firstname = "Benoit";
        user.lastname = "SIMARD";
        user.login = "bsimard";
        try {
            user.save();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage(), e);
        }
        render();
    }

    public static void delete() {
        User user = new User();
        user.email = "bsimard@logisima.com";
        user.firstname = "Benoit";
        user.lastname = "SIMARD";
        user.login = "bsimard";
        try {
            user.save();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage(), e);
        }
        render();
    }

}
