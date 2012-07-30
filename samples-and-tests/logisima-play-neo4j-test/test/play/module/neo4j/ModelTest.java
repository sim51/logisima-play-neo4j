package play.module.neo4j;

import java.sql.Date;

import models.User;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Neo4j;

public class ModelTest extends Neo4jUnit {

    @Test
    public void createObjectTest() throws Neo4jException {
        User user = new User();
        user.login = "bsimard";
        assertEquals(Boolean.TRUE, user.shouldBeSave);
        assertEquals(null, user.node);
    }

    @Test
    public void shouldBeSaveTest() throws Neo4jException {
        User user = new User();
        assertEquals(Boolean.FALSE, user.shouldBeSave);
        user.email = "qsdfqsdf";
        assertEquals(Boolean.TRUE, user.shouldBeSave);
        user = createDefaultUser();
        assertEquals(Boolean.FALSE, user.shouldBeSave);
        user.email = "contact@logisima.com";
        assertEquals(Boolean.TRUE, user.shouldBeSave);
        user.save();
        assertEquals(Boolean.FALSE, user.shouldBeSave);
    }

    @Test
    public void gettterSetterTest() throws Neo4jException {
        User user = new User();
        user.login = "bsimard";
        assertNull(user.node);
        assertNull(user.email);
        assertEquals(Boolean.TRUE, user.shouldBeSave);
        user.save();
        assertEquals("bsimard", user.login);
        user.login = "besim";
        assertEquals("besim", user.login);
        assertEquals("bsimard", user.node.getProperty("login", null));
        user.save();
        assertEquals("besim", user.node.getProperty("login", null));
        assertEquals("besim", user.login);
    }

    @Test
    public void saveTest() throws Neo4jException {
        // counting initiale node
        int init = countGraphNode();
        User user = createDefaultUser();
        assertEquals(Boolean.FALSE, user.shouldBeSave);
        assertNotNull(user.node);
        assertNotNull(user.key);
        // is there a suppl. node ?
        // counting initiale node
        int after = countGraphNode();
        assertEquals(init + 1, after);
    }

    @Test
    public void modifyTest() throws Neo4jException {
        User user = createDefaultUser();
        User user2 = User.getByKey(user.key);
        user2.age = 42;
        user2.devScore = new Long(0);
        user2.email = "toto@toto.com";
        user2.save();
        User after = User.getByKey(user.key);
        assertEquals(user.key, after.key);
        assertEquals("42", "" + after.age);
        assertEquals("0", "" + after.devScore);
        assertEquals("toto@toto.com", "" + after.email);
        // test case for issue #26
        assertEquals(user.login, after.login);
        assertEquals(user.birthday, after.birthday);
    }

    @Test
    public void getByKeyTest() throws Neo4jException {
        User user = createDefaultUser();
        User user2 = User.getByKey(user.key);

        assertEquals(user.key, user2.key);
        assertEquals(user.email, user2.email);
        assertEquals(user.firstname, user2.firstname);
        assertEquals(user.lastname, user2.lastname);
        assertEquals(user.login, user2.login);
        assertEquals(user.shouldBeSave, user2.shouldBeSave);

    }

    @Test
    public void indexTest() throws Neo4jException {
        Neo4j.clear();
        createDefaultUser();
        createUser("bsimard2@logisima.com", "Benoît2", "SIMARD", "bsimard2");
        int size = User.queryIndex("lastname", "lastname:SIMARD").size();
        assertEquals(2, size);
        size = User.queryIndex("lastname", "firstname:*").size();
        assertEquals(2, size);
    }

    @Test
    public void deleteTest() throws Neo4jException {
        int nbNode = countGraphNode();
        User user = createDefaultUser();
        // to be sure there is one more node
        assertEquals(nbNode + 1, countGraphNode());
        user.delete();
        assertEquals(nbNode, countGraphNode());
        Index<Node> indexNode = Neo4j.db().index().forNodes("USER_ID");
        Node node = indexNode.get("KEY", user.key).getSingle();
        assertNull(node);
    }

    @Test
    public void issue26Test() throws Neo4jException {
        User user = createDefaultUser();
        Long key = user.key;
        user.email = "toto@toto.com";
        user.save();

        // compare user with expected value
        assertEquals(new Integer(42), user.age);
        assertEquals("toto@toto.com", user.email);
        assertEquals("bsimard", user.login);
        assertEquals("Benoît", user.firstname);
        assertEquals("SIMARD", user.lastname);
        assertEquals(0, user.birthday.compareTo(new Date(1983, 2, 26)));
        assertTrue(user.isActive);

        // compare user with atabase value
        User db = User.getByKey(key);
        assertEquals(db.age, user.age);
        assertEquals(db.email, user.email);
        assertEquals(db.login, user.login);
        assertEquals(db.firstname, user.firstname);
        assertEquals(db.lastname, user.lastname);
        assertEquals(0, user.birthday.compareTo(db.birthday));
        assertEquals(db.isActive, user.isActive);
    }
}
