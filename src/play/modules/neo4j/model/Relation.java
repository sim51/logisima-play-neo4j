package play.modules.neo4j.model;


import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import play.Logger;
import play.modules.neo4j.annotation.RelatedTo;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.exception.Neo4jPlayException;
import play.modules.neo4j.util.Neo4j;

import java.util.*;

public class Relation<T extends Neo4jModel> implements Set<T> {
    public Neo4jModel parent;
    private List<T> elements;
    private RelatedTo relation;

    public Relation(Neo4jModel parent, RelatedTo relation) {
        this.parent = parent;
        this.relation = relation;
        this.elements = new ArrayList<T>();
    }

    public int size() {
        if (parent == null || parent.getNode() == null) {
            return 0;
        }
        return IteratorUtil.count(parent.getNode().getRelationships(getRelationshipType()));
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
        try {
            this.elements = asList();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage());
        }
        return this.elements.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        try {
            this.elements = asList();
        } catch (Neo4jException e) {
            Logger.error(e.getMessage());
        }

        return this.elements.toArray(ts);
    }

    @Override
    public boolean add(T element) {
        if (this.parent == null || this.parent.getNode() == null) {
            throw new Neo4jPlayException("Please save the parent node before adding childs");
        }
        if (exists(element)) {
            throw new Neo4jPlayException("Cet element est deja present");
        }
        Transaction tx = Neo4j.db().beginTx();
        try {
            element.getNode().createRelationshipTo(parent.getNode(), getRelationshipType());
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

    @Override
    public boolean remove(Object o) {
        T model = (T) o;
        deleteElement(model.getNode(), parent, getRelationshipType());
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
        for (T elt : this.elements) {
            deleteElement(elt.getNode(), parent, getRelationshipType());
        }
        this.elements.clear();
    }

    private static void deleteElement(Node node, Neo4jModel parent, DynamicRelationshipType relationshipType) {
        Transaction tx = Neo4j.db().beginTx();
        try {
            Iterator<Relationship> relationships = node.getRelationships(relationshipType).iterator();
            while (relationships.hasNext()) {
                Relationship r = relationships.next();
                if (r.getEndNode().equals(parent.getNode())) {
                    r.delete();
                }
            }
            tx.success();
        } finally {
            tx.finish();
        }
    }

    public List<T> asList
            () throws Neo4jException {
        if (elements == null || elements.size() == 0) {
            elements = new ArrayList<T>();
            org.neo4j.graphdb.Traverser t = elementsAsNodes();
            Iterator<Node> iterator = t.iterator();
            while (iterator.hasNext()) {
                Node noeud = iterator.next();
                elements.add((T) T.getByNode(noeud));
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
        return parent.getNode().traverse(
                org.neo4j.graphdb.Traverser.Order.BREADTH_FIRST,
                StopEvaluator.END_OF_GRAPH,
                ReturnableEvaluator.ALL_BUT_START_NODE,
                getRelationshipType(), getRelationshipDirection());
    }

    private Direction getRelationshipDirection() {
        return Direction.INCOMING;
    }

    private DynamicRelationshipType getRelationshipType() {
        return DynamicRelationshipType.withName(getRelationshipName());
    }

    private String getRelationshipName() {
        if (relation == null) {
            return this.parent.getClass().getSimpleName().toUpperCase() + "_" + this.getClass().getSimpleName().toUpperCase();
        } else {
            return relation.type();
        }
    }

    @Override
    public String toString() {
        String output = "Set[";
        for (T element : elements) {
            output += element.toString() + ", ";
        }
        return output;
    }
}
