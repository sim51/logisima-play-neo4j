package play.modules.neo4j.exception;

/**
 * Neo4j Reflection Exception class (it's a runtime exception by extending <code>PlayException</code>
 * 
 * @author bsimard
 * 
 */
public class Neo4jReflectionException extends Exception {

    /**
     * Construct a <code>Neo4jReflectionException</code> with the specified detail message.
     * 
     * @param msg the detail message
     */
    public Neo4jReflectionException(String message) {
        super(message);
    }

    /**
     * Construct a <code>Neo4jReflectionException</code> with the specified detail message and nested exception.
     * 
     * @param msg the detail message
     * @param cause the nested exception
     */
    public Neo4jReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a <code>Neo4jReflectionException</code> with the nested exception.
     * 
     * @param cause the nested exception
     */
    public Neo4jReflectionException(Throwable cause) {
        super(cause);
    }
}
