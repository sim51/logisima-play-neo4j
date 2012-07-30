package play.module.neo4j;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Fixtures;
import play.modules.neo4j.util.Neo4j;
import play.test.UnitTest;

public class FixturesTest extends UnitTest {

    @Test
    public void importYmlTest() throws Neo4jException {
        Fixtures.deleteDatabase();
        Fixtures.loadYml("data.yml");
        assertEquals(12, countGraphNode());
        assertEquals(14, countGraphRelationType());
    }

    private int countGraphNode() {
        int nb = 0;
        for (Node node : Neo4j.db().getAllNodes()) {
            nb++;
        }
        return nb;
    }

    private int countGraphRelationType() {
        int nb = 0;
        for (RelationshipType relationType : Neo4j.db().getRelationshipTypes()) {
            nb++;
        }
        return nb;
    }
}
