Play! framework for neo4j database

Features
=================

* embedded database (Only, no REST API for now)
* create your graph by annotation, module do mapping for you
* for each of your model, plugin generate an unique id (key)
* you can declare an index by annotation
* define relationships with annotation @RelatedTo
* define relationships class with properties with annotation @RelatedToVia

Installation
=================

add this to your application.conf

    ## NEO4J ----
    module.neo4j=lib/
    neo4j.path=db
    %test.neo4j.path=dbTest

Place *play-neo4j.jar* in your "lib" directory
look in sample app to know wich libs you must import

Usage
=================

Simple model
------------

**app/models/User.java**
``` java
    @Neo4jEntity
    public class User extends Neo4jModel {
        public String login;
    }
```
**app/controllers/Application.java**
``` java
    User johndoe = new User();
    johndoe.login = "johndoe";
    try {
        johndoe.save();
    } catch (Neo4jException e) {
        e.printStackTrace();
    }
```
**Graph**

Will create this graph
![graph 1](https://github.com/ZoRdAK/logisima-play-neo4j-fork/blob/master/docs/images/1.png?raw=true "graph1")

then when adding a second user
![graph 2](https://github.com/ZoRdAK/logisima-play-neo4j-fork/blob/master/docs/images/2.png?raw=true "graph2")


Simple relationship (working but in progress)
---------------------------------------------

**app/models/User.java**
``` java
    @Neo4jEntity
    public class User extends Neo4jModel {
        public String login;

        @RelatedTo(type = "KNOWS")
        public Set<User> friends;
    }
```
**app/controllers/Application.java**
``` java
    User johndoe = new User();
    johndoe.login = "johndoe";
    try {
        johndoe.save();
    } catch (Neo4jException e) {
        e.printStackTrace();
    }

    User foobar = new User();
    foobar.login = "foobar";
    try {
        foobar.save();
    } catch (Neo4jException e) {
        e.printStackTrace();
    }
    foobar.friends.add(johndoe);
```
**Graph**

Will create this graph
![graph 3](https://github.com/ZoRdAK/logisima-play-neo4j-fork/blob/master/docs/images/3.png?raw=true "graph3")

Relationship with attributes
----------------------------

**app/models/User.java**
``` java
    @Neo4jEntity
    public class User extends Neo4jModel {
        public String login;

        @RelatedTo(type = "KNOWS")
        public Set<User> friends;

        @RelatedToVia
        public Iterator<Bookmark> bookmarks;
    }
```
**app/models/Bookmark.java**
``` java
     @Neo4jEdge(type = "BOOKMARKED")
     public class Bookmark extends Neo4jRelationship {
         @StartNode
         public User user;

         @EndNode
         public Post post;

         public int stars;
     }
```
**app/models/Post.java**
``` java
    @Neo4jEntity
    public class Post extends Neo4jModel {
        public String title;
        public String content;
    }
```

**app/controllers/Application.java**
``` java
    User johndoe = new User();
    johndoe.login = "johndoe";
    johndoe.save();

    User foobar = new User();
    foobar.login = "foobar";
    foobar.save();
    foobar.friends.add(johndoe);

    Post myPost = new Post();
    myPost.title = "nice !";
    myPost.content = "blabla blabla bla";
    myPost.save();

    Bookmark bookmark = new Bookmark();
    bookmark.post = myPost;
    bookmark.user = foobar;
    bookmark.stars = 5;
    bookmark.save();
```
**Graph**

Will create this graph
![graph 4](https://github.com/ZoRdAK/logisima-play-neo4j-fork/blob/master/docs/images/4.png?raw=true "graph4")


See Junit Test in sample application for more !

TODO
=================
* import / export from an YML format
* Adding more documentation