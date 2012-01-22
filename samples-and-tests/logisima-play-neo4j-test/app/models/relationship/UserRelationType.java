package models.relationship;

import org.neo4j.graphdb.RelationshipType;

public enum UserRelationType implements RelationshipType {
    IS_A_CLASSMATE,
    IS_A_COLLEAGE,
    IS_FRIEND,
    IS_FAMILLY
}
