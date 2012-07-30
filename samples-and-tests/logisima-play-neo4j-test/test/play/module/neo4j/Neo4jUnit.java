package play.module.neo4j;

import java.sql.Date;

import models.User;

import org.junit.Test;
import org.neo4j.graphdb.Node;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Neo4j;
import play.test.UnitTest;

public class Neo4jUnit extends UnitTest {

    @Test
    public void testTest() {
        assertTrue(true);
    }

    protected User createDefaultUser() throws Neo4jException {
        return createUser("bsimard@logisima.com", "Benoît", "SIMARD", "bsimard", Boolean.TRUE);
    }

    protected User createDefaultUnsavedUser() throws Neo4jException {
        return createUser("bsimard@logisima.com", "Benoît", "SIMARD", "bsimard", Boolean.FALSE);
    }

    protected User createUser(String email, String firstname, String lastname, String login) throws Neo4jException {
        return createUser(email, firstname, lastname, login, Boolean.TRUE);
    }

    protected User createUser(String email, String firstname, String lastname, String login, Boolean save)
            throws Neo4jException {
        User user = new User();
        user.email = email;
        user.firstname = firstname;
        user.lastname = lastname;
        user.login = login;
        user.age = 42;
        user.isActive = Boolean.TRUE;
        user.birthday = new Date(1983, 2, 26);
        if (save)
            user.save();
        return user;
    }

    protected int countGraphNode() {
        int nb = 0;
        for (Node node : Neo4j.db().getAllNodes()) {
            nb++;
        }
        return nb;
    }

}
