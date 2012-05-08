package controllers;

import java.util.List;

import models.Touite;
import models.User;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Application extends Controller {

    public static void index() throws Neo4jException {
        User user = MySecurity.getConnectedUser();
        List<User> recos = user.getSimilarUser(user);
        List<Touite> touites = user.getFollowTouites();
        render("@index", user, recos, touites);
    }

    public static void user(Long key) throws Neo4jException {
        User user = (User) User.getByKey(key);
        List<User> recos = user.getSimilarUser(user);
        List<Touite> touites = user.getUserTouites();
        render("@index", user, recos, touites);
    }

    public static void touite(String text) throws Neo4jException {
        User user = MySecurity.getConnectedUser();
        user.touite(text);
        index();
    }

    public static void reTouite(Long key) throws Neo4jException {
        User user = MySecurity.getConnectedUser();
        user.reTouite(key);
        index();
    }

    public static void searchUser(String query) throws Neo4jException {
        List<User> users = User.queryIndex("lastname", "lastname:*" + query + "* OR firstname:*" + query
                + "* OR login:*" + query);
        render(users);
    }

    public static void userAvatar(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        if (user.avatar != null) {
            response.setContentTypeIfNotSet(user.avatar.type());
            renderBinary(user.avatar.getFile());
        }
        else {
            notFound();
        }
    }

    public static void follow(Long key) throws Neo4jException {
        User user = MySecurity.getConnectedUser();
        User friend = User.getByKey(key);
        user.friends.add(friend);
        user.save();
        index();
    }

}
