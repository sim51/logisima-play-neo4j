package jobs;

import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.modules.neo4j.util.Fixtures;

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {
        Logger.info("Delete database & load yml file");
        Fixtures.deleteDatabase();
        Fixtures.loadYml("data.yml");
    }
}
