package play.modules.neo4j.exception;

import play.exceptions.PlayException;

/**
 * Neo4j Play Exception class (it's a runtime exception by extending <code>PlayException</code>)
 * 
 * @author bsimard
 * 
 */
public class Neo4jPlayException extends PlayException {

    public Neo4jPlayException(String message) {
        super(message);
    }

    @Override
    public String getErrorTitle() {
        return "Neo4j Exception";
    }

    @Override
    public String getErrorDescription() {
        return getMessage();
    }

}
