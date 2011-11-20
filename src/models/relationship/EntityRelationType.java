package models.relationship;

import org.neo4j.graphdb.RelationshipType;

public enum EntityRelationType implements RelationshipType {
    USERS_REFERENCE,
    USER,
    RESTAURENTS_REFERENCE,
    RESTAURENT
}
