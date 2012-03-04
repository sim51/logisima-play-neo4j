package controllers;

import models.User;
import play.Logger;
import play.modules.neo4j.exception.Neo4jException;
import play.mvc.Controller;

public class UserDTO extends Controller {

    public static void edit(Long key) throws Neo4jException {
        User user = User.getByKey(key);
        models.UserDTO userDTO = new models.UserDTO();
        userDTO.setUser(user);
        userDTO.setUniversityName("test");
        render(userDTO);
    }

    public static void save(models.UserDTO userDTO) throws Neo4jException {
        Logger.info(userDTO.user.toString());
        render("@edit", userDTO);
    }

}
