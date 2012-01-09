package controllers;

import models.User;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;

public class Application extends Controller {

    public static void index() {
        User johndoe = new User();
        johndoe.email = "johndoe@mail.com";
        johndoe.login = "johndoe";
        try {
            johndoe.save();
        } catch (Neo4jException e) {
            e.printStackTrace();
        }

        User toto = new User();
        toto.email = "toto@mail.com";
        toto.login = "toto";


        try {
            toto.save();
        } catch (Neo4jException e) {
            e.printStackTrace();
        }
        toto.friends.add(johndoe);

        display(toto.getKey());
    }

    public static void display(Long key) {
        try {
            User user = User.getByKey(key);
            for (User u : user.friends.toArray(new User[1])) {
                System.out.println(u.toString());
            }
        } catch (Neo4jException e) {
            e.printStackTrace();
        }
    }

}