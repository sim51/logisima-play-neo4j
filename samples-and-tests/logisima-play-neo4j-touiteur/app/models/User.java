package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;

import play.db.jpa.Blob;
import play.modules.neo4j.annotation.Neo4jIndex;
import play.modules.neo4j.annotation.Neo4jRelatedTo;
import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.model.Neo4jModel;
import play.modules.neo4j.util.Neo4j;

import com.google.gson.Gson;

public class User extends Neo4jModel {

    @Neo4jIndex(value = "login")
    public String     login;

    public String     email;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     firstname;

    @Neo4jIndex(value = "lastname", type = "fulltext")
    public String     lastname;

    public Integer    nbTouite = 0;

    public Date       birthday;

    public Blob       avatar;

    @Neo4jRelatedTo(value = "IS_FRIEND", direction = Direction.OUTGOING, lazy = true)
    public List<User> friends;

    @Neo4jRelatedTo(value = "IS_FRIEND", direction = Direction.INCOMING, lazy = true)
    public List<User> followers;

    public String toString() {
        return new Gson().toJson(this);
    }

    public static User getConnectedUser(Long key) throws Neo4jException {
        User user = (User) User.getByKey(key);
        return user;
    }

    public List<User> getRecommandations() throws Neo4jException {
        ExecutionEngine engine = new ExecutionEngine(Neo4j.db());
        //@formatter:off
        ExecutionResult result = engine.execute("" +
        		"START me=node(" + this.node.getId() + ") " +
                "MATCH me-[:IS_FRIEND]->friend-[:IS_FRIEND]->friend_of_friend, me-[r?:IS_FRIEND]->friend_of_friend " +
                "WHERE (r IS NULL) and not(friend_of_friend.key = me.key) " +
                "RETURN friend_of_friend, COUNT(*) " +
                "ORDER BY COUNT(*) DESC, friend_of_friend.key " +
                "LIMIT 3");
        //@formatter:on
        List<User> recos = new ArrayList<User>();
        Iterator<Node> column = result.columnAs("friend_of_friend");
        for (Node node : IteratorUtil.asIterable(column)) {
            User user = (User) User.getByNode(node);
            if (user != null) {
                recos.add(user);
            }
        }
        return recos;
    }

    public List<Touite> getUserTouites() throws Neo4jException {
        ExecutionEngine engine = new ExecutionEngine(Neo4j.db());
        //@formatter:off
        ExecutionResult result = engine.execute("" +
                "START me=node(" + this.node.getId() + ") " +
                "MATCH me-[:NEXT*1..10]->touite " +
                "RETURN touite " +
                "ORDER BY touite.created DESC " +
                "LIMIT 10");
        //@formatter:on
        List<Touite> touites = new ArrayList<Touite>();
        Iterator<Node> column = result.columnAs("touite");
        for (Node node : IteratorUtil.asIterable(column)) {
            if (node != null && node.getProperty("key", null) != null) {
                Touite touite = (Touite) Touite.getByNode(node);
                if (touite != null) {
                    touites.add(touite);
                }
            }
        }
        return touites;
    }

    public List<Touite> getFollowTouites() throws Neo4jException {
        ExecutionEngine engine = new ExecutionEngine(Neo4j.db());
        //@formatter:off
        ExecutionResult result = engine.execute("" +
                "START me=node(" + this.node.getId() + ") " +
                "MATCH me-[:IS_FRIEND*0..1]->friend-[:NEXT*1..50000]->touite, touite-[r?:RETOUITE_OF]->touite " +
                "WHERE r is null " +
                "RETURN touite " +
                "ORDER BY touite.created DESC " +
                "SKIP 0 " +
                "LIMIT 10");
        //@formatter:on
        List<Touite> touites = new ArrayList<Touite>();
        Iterator<Node> column = result.columnAs("touite");
        for (Node node : IteratorUtil.asIterable(column)) {
            if (node != null && node.getProperty("key", null) != null) {
                Touite touite = (Touite) Touite.getByNode(node);
                if (touite != null) {
                    touites.add(touite);
                }
            }
        }
        return touites;
    }

    public Touite getLastTouite() throws Neo4jException {
        Touite touite = null;
        Node userNode = this.node;
        Iterator<org.neo4j.graphdb.Relationship> relationships = userNode.getRelationships(RelationType.NEXT,
                org.neo4j.graphdb.Direction.OUTGOING).iterator();
        while (relationships.hasNext()) {
            Node touiteNode = relationships.next().getEndNode();
            touite = Touite.getByNode(touiteNode);
        }
        return touite;
    }

    public void reTouite(Long key) throws Neo4jException {
        Transaction tx = Neo4j.db().beginTx();
        try {
            Touite lastTouite = this.getLastTouite();
            Touite touite = Touite.getByKey(key);
            Touite retouite = new Touite();
            retouite.text = touite.text;
            retouite.created = new Date();
            retouite.save();

            // delete the previous NEXT relationship
            Relationship next = this.node.getSingleRelationship(RelationType.NEXT, Direction.OUTGOING);
            next.delete();

            // create next link between user & touite
            this.node.createRelationshipTo(retouite.node, RelationType.NEXT);

            // relink lasttouite with the new one
            retouite.node.createRelationshipTo(lastTouite.node, RelationType.NEXT);

            // create author link
            retouite.node.createRelationshipTo(touite.getAuthor().node, RelationType.AUTHOR);

            // create retouite link
            retouite.node.createRelationshipTo(touite.node, RelationType.RETOUITE_OF);

            // +1
            if (this.nbTouite != null) {
                this.nbTouite += 1;
            }
            else {
                this.nbTouite = 1;
            }
            this.save();

            tx.success();
        } finally {
            tx.finish();
        }
    }

    public void touite(String text) throws Neo4jException {
        Transaction tx = Neo4j.db().beginTx();
        try {
            Touite touite = new Touite();
            touite.text = text;
            touite.created = new Date();
            touite.save();

            Touite lastTouite = this.getLastTouite();
            // delete the previous NEXT relationship
            Relationship next = this.node.getSingleRelationship(RelationType.NEXT, Direction.OUTGOING);
            next.delete();

            // create next link between user & touite
            this.node.createRelationshipTo(touite.node, RelationType.NEXT);

            // create author link
            touite.node.createRelationshipTo(this.node, RelationType.AUTHOR);

            // relink lasttouite with the new one
            touite.node.createRelationshipTo(lastTouite.node, RelationType.NEXT);

            // +1
            if (this.nbTouite != null) {
                this.nbTouite += 1;
            }
            else {
                this.nbTouite = 1;
            }
            this.save();

            tx.success();
        } finally {
            tx.finish();
        }
    }

    public List<User> getSimilarUser(User user) throws Neo4jException {
        ExecutionEngine engine = new ExecutionEngine(Neo4j.db());
        //@formatter:off
        ExecutionResult result = engine.execute("" +
                "START user=node(" + user.node.getId() + ") " +
                "MATCH reco-[:IS_FRIEND*1..3]->user, user-[r?:IS_FRIEND]->reco " +
                "WHERE r IS NULL  and not(reco.key = user.key) " +
                "RETURN reco, COUNT(*) " +
                "ORDER BY COUNT(*) DESC, reco.key " +
                "LIMIT 3");
        //@formatter:on
        List<User> recos = new ArrayList<User>();
        Iterator<Node> column = result.columnAs("reco");
        for (Node node : IteratorUtil.asIterable(column)) {
            recos.add((User) User.getByNode(node));
        }
        return recos;
    }
}
