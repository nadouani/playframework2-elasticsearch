package controllers;

import Models.Merda;
import com.github.eduardofcbg.plugin.es.Finder;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * Created by eduardo on 17-06-2015.
 */
public class C extends Controller {

    public static Result ole() {
        Finder f = Merda.find;
        if (f == null) {
            return ok("merda");
        } else return ok("simsims");
    }

    public static Result oo() {
        return ok(new Merda("mee").getO() + "");
    }

}
