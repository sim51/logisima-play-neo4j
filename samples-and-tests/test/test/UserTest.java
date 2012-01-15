import models.Bookmark;
import models.Post;
import models.User;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import play.modules.neo4j.exception.Neo4jException;
import play.test.UnitTest;

import java.util.List;

public class UserTest extends UnitTest {
    private User user;

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() throws Neo4jException {
        List<User> all = User.findAll();
        for (User u : all) {
            for (User friend : u.friends) {
                friend.delete();
            }
            u.delete();
        }
    }

    @AfterClass
    public static void cleanUpGraph() {
        User.cleanUp();
    }

    @Test
    public void addUser() throws Neo4jException {
        assertEquals(0, User.findAll().size());
        user = prepareUser();

        assertNotNull(user.getKey());
        assertNotNull(user.getNode());

        User userBis = User.getByKey(user.getKey());
        assertEquals(user.email, userBis.email);
        assertEquals(user.login, userBis.login);
        assertEquals(1, User.findAll().size());
        assertFalse(user.bookmarks.hasNext());
    }

    @Test
    public void editUser() throws Neo4jException {
        user = prepareUser();

        user.login = "new login";
        user.save();

        User userBis = User.getByKey(user.getKey());
        assertEquals("new login", userBis.login);
        assertEquals(1, User.findAll().size());
    }

    @Test
    public void removeUser() throws Neo4jException {
        user = prepareUser();

        Long key = user.getKey();
        user.delete();

        assertNull(User.getByKey(key));
        assertEquals(0, User.findAll().size());
    }

    @Test
    public void addFriends() throws Neo4jException {
        user = addUserWithFriends();

        User userBis = User.getByKey(user.getKey());
        assertEquals(2, userBis.friends.size());
        assertEquals(3, User.findAll().size());
    }


    @Test
    public void addFriendsAndRemoveOne() throws Neo4jException {
        user = addUserWithFriends();

        User firstFriend = user.friends.iterator().next();
        user.friends.remove(firstFriend);

        User userBis = User.getByKey(user.getKey());
        assertEquals(1, userBis.friends.size());
        assertFalse(user.friends.contains(firstFriend));
    }


    @Test
    public void addBookmark() throws Neo4jException {
        user = prepareUser();
        assertFalse(user.bookmarks.hasNext());
        Post post = preparePost();

        Bookmark bookmark = prepareBookmark(post);

        User userBis = User.getByKey(user.getKey());
        assertBookmarkAdded(bookmark, userBis);

    }

    @Test
    public void removeBookmark() throws Neo4jException {
        user = prepareUser();
        assertFalse(user.bookmarks.hasNext());
        Post post = preparePost();

        Bookmark bookmark = prepareBookmark(post);

        User userBis = User.getByKey(user.getKey());
        assertTrue(userBis.bookmarks.hasNext());
        Bookmark firstBookmark = userBis.bookmarks.next();
        assertEquals(bookmark.stars, firstBookmark.stars);
        assertEquals(bookmark.post.title, firstBookmark.post.title);
        assertEquals(bookmark.user.login, firstBookmark.user.login);

        firstBookmark.delete();
        User userTer = User.getByKey(user.getKey());
        assertFalse(userTer.bookmarks.hasNext());
    }

    private User prepareUser() throws Neo4jException {
        return prepareUser("johndoe");
    }

    private User prepareUser(String login) throws Neo4jException {
        User newUser = new User();
        newUser.email = login + "@mail.com";
        newUser.login = login;
        newUser.save();
        return newUser;
    }


    private User addUserWithFriends() throws Neo4jException {
        User userWithFriends = prepareUser();
        User peter = prepareUser("peter");
        User eliot = prepareUser("eliot");
        assertEquals(0, userWithFriends.friends.size());

        userWithFriends.friends.add(peter);
        userWithFriends.friends.add(eliot);
        userWithFriends.save();
        return userWithFriends;
    }

    private void assertBookmarkAdded(Bookmark bookmark, User userBis) {
        assertTrue(userBis.bookmarks.hasNext());
        Bookmark firstBookmark = userBis.bookmarks.next();
        assertEquals(bookmark.stars, firstBookmark.stars);
        assertEquals(bookmark.post.title, firstBookmark.post.title);
        assertEquals(bookmark.user.login, firstBookmark.user.login);
    }

    private Bookmark prepareBookmark(Post post) throws Neo4jException {
        Bookmark bookmark = new Bookmark();
        bookmark.post = post;
        bookmark.user = user;
        bookmark.stars = 5;
        bookmark.save();
        return bookmark;
    }

    private Post preparePost() throws Neo4jException {
        Post post = new Post();
        post.title = "Cool post";
        post.content = "My super article";
        post.save();
        return post;
    }

}
