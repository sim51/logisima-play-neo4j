package play.module.neo4j;

import models.User;

import org.junit.Test;
import org.neo4j.graphdb.Node;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Neo4j;
import play.test.UnitTest;

public class DatabaseTest extends UnitTest {

    @Test
    public void deleteDbTest() throws Neo4jException {
        // adding an object into DB
        // TODO: replace this by the import !
        User user = new User();
        user.email = "bsimard@logisima.com";
        user.firstname = "Benoit";
        user.lastname = "SIMARD";
        user.login = "bsimard";
        user.save();

        // calling clear database
        Neo4j.clear();

        // counting node
        int i = 0;
        for (Node node : Neo4j.db().getAllNodes()) {
            i++;
        }

        assertEquals(1, i);
    }

}
