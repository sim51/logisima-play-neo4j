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
        for (Node node : usersIndex.query("lastname:*" + query + "* OR firstname:*" + query + "*")) {
            User user = new User();
            user.setNode(node);
            users.add(user);
        }
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

    public static void userSerachUserRelation(Long key, String query) throws Neo4jException {
        User user = User.getByKey(key);
        List<User> users = new ArrayList<User>();
        IndexManager index = Neo4j.db().index();
        Index<Node> usersIndex = index.forNodes("lastname");
        for (Node node : usersIndex.query("lastname", query)) {
            User usr = new User();
            usr.setNode(node);
            users.add(usr);
        }
        render("Application/user.html", user, users);
    }

    public static void userAddRelation(Long key, Long related, int type) throws Neo4jException {
        User user = User.getByKey(key);
        User relatedUser = User.getByKey(related);
        switch (type) {
            case 1:
                // friend
                user.friends.add(relatedUser);
                break;
            case 2:
                // familly
                user.famillies.add(relatedUser);
                break;
            case 3:
                // colleages
                user.colleages.add(relatedUser);
                break;
            case 4:
                // classmate
                List<User> classmates = user.classmates;
                user.classmates.add(relatedUser);
                break;
        }
        user.save();
        user(key);
    }

    public static void userDeleteRelation(Long key, Long related, int type) throws Neo4jException {
        User user = User.getByKey(key);
        switch (type) {
            case 1:
                // friend
                for (int i = 0; i < user.friends.size(); i++) {
                    if (user.friends.get(i).getKey() == related) {
                        user.friends.remove(i);
                    }
                }
                break;
            case 2:
                // familly
                for (int i = 0; i < user.famillies.size(); i++) {
                    if (user.famillies.get(i).getKey() == related) {
                        user.famillies.remove(i);
                    }
                }
                break;
            case 3:
                // colleages
                for (int i = 0; i < user.colleages.size(); i++) {
                    if (user.colleages.get(i).getKey() == related) {
                        user.colleages.remove(i);
                    }
                }
                break;
            case 4:
                // classmate
                for (int i = 0; i < user.classmates.size(); i++) {
                    if (user.classmates.get(i).getKey() == related) {
                        user.classmates.remove(i);
                    }
                }
                break;
        }
        user.save();
        user(key);
    }
}
