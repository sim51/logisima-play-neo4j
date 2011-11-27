package play.modules.neo4j.exception;

/**
 * Neo4j Exception class.
 * 
 * @author bsimard
 * 
 */
public class Neo4jException extends Exception {

    /**
     * Construct a <code>Neo4jException</code> with the specified detail message.
     * 
     * @param msg the detail message
     */
    public Neo4jException(String message) {
        super(message);
    }

    /**
     * Construct a <code>Neo4jException</code> with the specified detail message and nested exception.
     * 
     * @param msg the detail message
     * @param cause the nested exception
     */
    public Neo4jException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construct a <code>Neo4jException</code> with the nested exception.
     * 
     * @param cause the nested exception
     */
    public Neo4jException(Throwable cause) {
        super(cause);
    }
}
