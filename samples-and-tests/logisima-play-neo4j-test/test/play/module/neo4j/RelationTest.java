package play.module.neo4j;

import java.util.List;

import models.User;

import org.junit.BeforeClass;
import org.junit.Test;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Fixtures;

public class RelationTest extends Neo4jUnit {

    @BeforeClass
    public static void tearsUp() {
        Fixtures.deleteDatabase();
        Fixtures.loadYml("data.yml");
    }

    @Test
    public void lazyLoadingTest() throws Neo4jException {
        List<User> users = User.findAll();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.login.equals("ben@gmail.com")) {
                assertEquals(1, user.classmates.size());
                assertEquals("osecher@gmail.com", user.classmates.get(0).email);
            }
        }
    }

    @Test
    public void simpleLoadingTest() throws Neo4jException {
        List<User> users = User.findAll();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.login.equals("ben@gmail.com")) {
                assertEquals(1, user.friends.size());
                assertEquals("osecher@gmail.com", user.friends.get(0).email);
            }
        }
    }

    @Test
    public void reverseLoadingTest() throws Neo4jException {
        List<User> users = User.findAll();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            if (user.login.equals("osecher@gmail.com")) {
                assertEquals(1, user.reversefriends.size());
                assertEquals("ben@gmail.com", user.friends.get(0).email);
            }
        }
    }

    @Test
    public void loadRelationOnUnsavedModel() throws Neo4jException {
        User user = createDefaultUnsavedUser();
        List<User> classmates = user.classmates;
        assertEquals(0, classmates.size());
    }

    @Test
    public void loadRelationWithBothDirection() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        assertEquals(2, user.friends.size());
        assertEquals(1, user.reversefriends.size());
        assertEquals(3, user.allFriends.size());
    }
}
