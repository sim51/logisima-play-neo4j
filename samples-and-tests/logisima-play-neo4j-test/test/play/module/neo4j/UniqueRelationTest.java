package play.module.neo4j;

import models.Address;
import models.Job;
import models.User;

import org.junit.BeforeClass;
import org.junit.Test;

import play.modules.neo4j.exception.Neo4jException;
import play.modules.neo4j.util.Fixtures;
import play.test.UnitTest;

public class UniqueRelationTest extends UnitTest {

    @BeforeClass
    public static void tearsUp() {
        Fixtures.deleteDatabase();
        Fixtures.loadYml("data.yml");
    }

    @Test
    public void simpleLoadingTest() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        Address address = user.address;
        assertEquals("My address", address.title);
    }

    @Test
    public void lineLoadingTest() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        Job currentJob = user.job;
        assertNotNull(currentJob);
        assertEquals("My last job", currentJob.title);
        Job previousJob = currentJob.job;
        assertNotNull(previousJob);
        assertEquals("My last-1 job", previousJob.title);
        Job previousJob2 = previousJob.job;
        assertNotNull(previousJob2);
        assertEquals("My last-2 job", previousJob2.title);

    }

    @Test
    public void saveWithNoChange() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        assertEquals("My address", after.address.title);
        assertEquals("My last job", after.job.title);
    }

    @Test
    public void saveNormalWithNullBefore() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*bossard* OR firstname:*bossard*").get(0);
        Address address = new Address();
        address.title = "my address";
        address.save();
        user.address = address;
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*bossard* OR firstname:*bossard*").get(0);
        assertEquals("my address", after.address.title);
    }

    @Test
    public void saveNormalWithNotNullBefore() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        Address address = new Address();
        address.title = "my address 2";
        address.save();
        user.address = address;
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        assertEquals(user.address.title, after.address.title);
        assertEquals("my address 2", after.address.title);
    }

    @Test
    public void saveNormalWithDeletion() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        user.address = null;
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        assertNull(after.address);
    }

    @Test
    public void saveLineWithNullBefore() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*bossard* OR firstname:*bossard*").get(0);
        Job job = new Job();
        job.title = "my job";
        job.save();
        user.job = job;
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*bossard* OR firstname:*bossard*").get(0);
        assertEquals(user.job.title, after.job.title);
    }

    @Test
    public void saveLineWithNotNullBefore() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        Job job = new Job();
        job.title = "my job";
        job.save();
        user.job = job;
        user.save();
        User after = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        assertEquals(user.job.title, after.job.title);
        assertEquals("my job", after.job.title);
    }

    @Test
    public void saveLineWithDeletionException() {
        User user = (User) User.queryIndex("lastname", "lastname:*beno* OR firstname:*beno*").get(0);
        user.job = null;
        try {
            user.save();
        } catch (Neo4jException e) {
            assertEquals(
                    "play.modules.neo4j.exception.Neo4jException: You can't have a null value when line mode is activated. If you want to delete the chain, you have to do it item by item !",
                    e.getMessage());
        }
    }

    @Test
    public void saveLineWithDeletion() throws Neo4jException {
        User user = (User) User.queryIndex("lastname", "lastname:*olivier* OR firstname:*olivier*").get(0);
        Job job = new Job();
        job.title = "my job";
        job.save();
        user.job = job;
        user.save();
        User temp = (User) User.queryIndex("lastname", "lastname:*olivier* OR firstname:*olivier*").get(0);
        temp.job = null;
        temp.save();
        User after = (User) User.queryIndex("lastname", "lastname:*olivier* OR firstname:*olivier*").get(0);
        assertNull(after.job);
    }

}
