package controllers.module.neo4j;

import play.mvc.Controller;

public class Neo4jController extends Controller {

    public static void console() {
        String url = "http://";
        if (request.secure) {
            url = "https://";
        }
        url += request.domain;
        url += ":7474/webadmin/";
        redirect(url);
    }

}
