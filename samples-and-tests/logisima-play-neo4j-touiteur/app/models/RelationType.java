package models;

import org.neo4j.graphdb.RelationshipType;

public enum RelationType implements RelationshipType {
    IS_FRIEND,
    NEXT,
    AUTHOR,
    RETOUITE_OF
}
