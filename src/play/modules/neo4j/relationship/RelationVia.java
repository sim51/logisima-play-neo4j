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
package play.modules.neo4j.relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import play.modules.neo4j.annotation.Neo4jEndNode;
import play.modules.neo4j.annotation.Neo4jRelatedToVia;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.model.Neo4jModel;
import play.modules.neo4j.util.Neo4j;

public class RelationVia<T extends Neo4jModel> implements Set<T> {

    private Neo4jModel   parent;
    private List<T>      elements;
    private Neo4jRelatedToVia relation;

    public RelationVia(Neo4jModel parent, Neo4jRelatedToVia relation) {
        this.parent = parent;
        this.relation = relation;
    }

    public int size() {
        return IteratorUtil.count(getStartNode().getRelationships(getRelationshipType()));
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return this.elements.iterator();
    }

    @Override
    public Object[] toArray() {
        return this.elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return this.elements.toArray(ts);
    }

    @Override
    public boolean add(T element) {
        if (exists(element)) {
            throw new Neo4jPlayException("Cet element est deja present");
        }
        Transaction tx = Neo4j.db().beginTx();
        try {
            Relationship relationship = getEndNode(element).createRelationshipTo(getStartNode(), getRelationshipType());
            // Add attributes
            for (java.lang.reflect.Field field : element.getClass().getFields()) {
                if (field.getAnnotation(Neo4jEndNode.class) == null) {
                    relationship.setProperty(field.getName(), field.get(element));
                }
            }

            if (elements == null) {
                elements = new ArrayList<T>();
            }
            elements.add(element);
            tx.success();
        } catch (Exception e) {
            return false;
        } finally {
            tx.finish();
        }

        return true;
    }

    private Node getStartNode() {
        return parent.getNode();
    }

    private Node getEndNode(T element) {
        // Find end Node in class
        Neo4jEndNode endNode = null;
        for (java.lang.reflect.Field field : element.getClass().getFields()) {
            endNode = field.getAnnotation(Neo4jEndNode.class);
            if (endNode != null) {
                try {
                    Object endObject = field.get(element);
                    if (endObject == null) {
                        throw new Neo4jPlayException("Oups, Neo4jEndNode undefined");
                    }
                    return ((Neo4jModel) endObject).getNode();
                } catch (IllegalAccessException e) {
                    throw new Neo4jPlayException("Please @Neo4jEndNode must extend Neo4jModel");
                } catch (ClassCastException e) {
                    throw new Neo4jPlayException("Oups  ");
                }
            }
        }
        throw new Neo4jPlayException("Please Annotate your model with @Neo4jEndNode field");
    }

    @Override
    public boolean remove(Object o) {
        return this.elements.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        return this.elements.containsAll(objects);
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        return this.elements.addAll(ts);
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        return this.elements.retainAll(objects);
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        return this.elements.removeAll(objects);
    }

    @Override
    public void clear() {
        this.elements.clear();
    }

    public List<T> asList() throws Neo4jException {
        if (elements == null) {
            elements = new ArrayList<T>();
            org.neo4j.graphdb.Traverser t = elementsAsNodes();
            Iterator<Node> iterator = t.iterator();
            while (iterator.hasNext()) {
                Node noeud = iterator.next();
                elements.add(T.<T> getByKey((Long) noeud.getProperty("key")));
            }
        }
        return elements;
    }

    private boolean exists(T element) {
        org.neo4j.graphdb.Traverser t = elementsAsNodes();
        Iterator<Node> iterator = t.iterator();
        while (iterator.hasNext()) {
            Node noeud = iterator.next();
            if (noeud.equals(element.getNode())) {
                return true;
            }
        }
        return false;
    }

    private org.neo4j.graphdb.Traverser elementsAsNodes() {
        return getStartNode().traverse(org.neo4j.graphdb.Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH,
                ReturnableEvaluator.ALL_BUT_START_NODE, getRelationshipType(), getRelationshipDirection());
    }

    private Direction getRelationshipDirection() {
        return Direction.INCOMING;
    }

    private DynamicRelationshipType getRelationshipType() {
        return DynamicRelationshipType.withName(getRelationshipName());
    }

    private String getRelationshipName() {
        if (relation == null) {
            return this.parent.getClass().getSimpleName().toUpperCase() + "_"
                    + this.getClass().getSimpleName().toUpperCase();
        }
        else {
            return "coucou";// relation.type();
        }
        // Récupérer la valeur de l'annotation et si vide => PARENT_ENFANT
    }

    private enum Relations implements RelationshipType {
        PARTICIPE
    }
}
