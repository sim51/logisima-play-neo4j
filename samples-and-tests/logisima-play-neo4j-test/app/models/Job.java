package models;

import play.modules.neo4j.annotation.Neo4jUniqueRelation;
import play.modules.neo4j.model.Neo4jModel;

public class Job extends Neo4jModel {

    public String title;

    @Neo4jUniqueRelation(value = "NEXT_JOB", line = true)
    public Job    job;

}
