package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.FilterBuilders;
import play.libs.F;

import java.util.List;

/**
 * Created by eduardo on 17-05-2015.
 */
public class Person extends Index {

    String name;
    int age;

    public static final Finder<Person> find = new Finder<Person>(Person.class);

    @JsonCreator
    public Person(@JsonProperty("name") String name, @JsonProperty("age") int age) {
        this.name = name;
        this.age = age;
    }

    public F.Promise<IndexResponse> index() {
        return find.index(this);
    }

    public static F.Promise<Person> get(String id) {
        return find.get(id);
    }

    public static F.Promise<List<Person>> getAdults() {
        return find.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(18)));
    }

    public static F.Promise<UpdateResponse> incrementAge(String id) {
        return find.update(id, original -> {
            original.age++;
            return original;
        });
    }

}
