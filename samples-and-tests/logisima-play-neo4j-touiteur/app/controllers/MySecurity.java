package controllers;

import java.util.List;

import models.User;
import play.Logger;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Scope;
import controllers.Secure.Security;

public class MySecurity extends Security {

    public static boolean check(String profile) {
        return true;
    }

    public static boolean authenticate(String username, String password) {
        Logger.debug("[MySecurity]: onAutenticated method");
        if (username.equals(password)) {
            session.put("username", username);
            return true;
        }
        else {
            return false;
        }
    }

    public static void onDisconnected() {
        Logger.debug("[MySecurity]: onAutenticated method");
        Scope.Session.current().clear();
        try {
            Application.index();
        } catch (Neo4jException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static User getConnectedUser() {
        String username = MySecurity.connected();
        Logger.info(username);
        List<User> users = User.queryIndex("login", "login:*" + username.toLowerCase() + "*");
        if (users.size() > 0) {
            return users.get(0);
        }
        else {
            return null;
        }
    }

}
