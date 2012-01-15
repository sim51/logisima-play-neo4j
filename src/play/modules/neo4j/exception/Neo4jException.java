/**
 * This file is part of logisima-play-neo4j.
 *
 * logisima-play-neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logisima-play-neo4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with logisima-play-neo4j. If not, see <http://www.gnu.org/licenses/>.
 * 
 * @See https://github.com/sim51/logisima-play-neo4j
 */
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
