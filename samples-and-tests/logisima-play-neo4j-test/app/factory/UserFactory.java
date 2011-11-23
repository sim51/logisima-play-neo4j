package factory;

import models.relationship.EntityRelationType;
import play.modules.neo4j.annotation.Neo4jFactory;
import play.modules.neo4j.util.AbstractNeo4jFactory;

/**
 * Factory class for User node.
 * 
 * @author bsimard
 * 
 */
@Neo4jFactory(clazz = EntityRelationType.class, root2ref = "USERS_REFERENCE", ref2node = "USER")
public class UserFactory extends AbstractNeo4jFactory {

    /**
     * Constructor of the User Factory.
     */
    public UserFactory() {
        super();
    }

}
