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

import play.exceptions.PlayException;

/**
 * Neo4j Play Exception class (it's a runtime exception by extending <code>PlayException</code>)
 * 
 * @author bsimard
 */
public class Neo4jPlayException extends PlayException {

    public Neo4jPlayException(String message) {
        super(message);
    }

    public Neo4jPlayException(Exception e) {
        super(e.getMessage(), e);
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
