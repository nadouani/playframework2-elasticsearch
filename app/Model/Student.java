package Model;

import com.github.eduardofcbg.plugin.es.ES;
import com.google.inject.Inject;

/**
 * Created by eduardo on 22-06-2015.
 */
public class Student {

    public String name;

    @Inject
    ES es;

    public Student(String name) {
        this.name = name;
    }

    public String name() {
        return es.indexName();
    }

}
