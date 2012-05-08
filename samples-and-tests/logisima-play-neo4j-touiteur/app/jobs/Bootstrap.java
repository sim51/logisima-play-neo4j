package jobs;

import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job {

    public void doJob() {
        // Logger.info("Delete database & load yml file");
        // Fixtures.deleteDatabase();
        // Fixtures.loadYml("data.yml");
    }
}
