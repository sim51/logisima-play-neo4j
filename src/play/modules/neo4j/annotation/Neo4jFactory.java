package play.modules.neo4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import models.relationship.EntityRelationType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Neo4jFactory {

    EntityRelationType root2ref();

    EntityRelationType ref2node();
}
