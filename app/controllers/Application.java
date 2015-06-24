package controllers;

import com.github.eduardofcbg.plugin.es.ES;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * Created by eduardo on 22-06-2015.
 */
public class Application extends Controller {

    @Inject
    static ES es;

    public static Result test() {
        return ok(es.indexName());
    }

}
