package play.module.neo4j;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.tooling.GlobalGraphOperations;

import play.Logger;
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
        assertEquals(11, countGraphRelationType());
    }

    private int countGraphNode() {
        int nb = 0;
        for (Node node : GlobalGraphOperations.at(Neo4j.db()).getAllNodes()) {
            Logger.info("Node " + node.getId() + " => " + node.toString());
            nb++;
        }
        return nb;
    }

    private int countGraphRelationType() {
        int nb = 0;
        for (RelationshipType relationType : GlobalGraphOperations.at(Neo4j.db()).getAllRelationshipTypes()) {
            Logger.info("Relation " + relationType.name());
            nb++;
        }
        return nb;
    }
}
