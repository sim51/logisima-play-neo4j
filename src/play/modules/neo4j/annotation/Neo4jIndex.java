package play.modules.neo4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Neo4j Index annotation. Will create a neo4j index if a field is annotated this annotation.
 * 
 * @author bsimard
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Neo4jIndex {

    String value() default "";
}
