package models;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.index.query.FilterBuilders;

import play.libs.F.Promise;

import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Result;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

@Index.Entity(type="person")
public class Person extends Index {

    public String name;
    public int age;

    public static final Finder<Person> find = new Finder<Person>(Person.class);

    @JsonCreator
    public Person(@JsonProperty("name") String name, @JsonProperty("age") int age) {
        this.name = name;
        this.age = age;
    }

    public Promise<Result<Person>> index() throws JsonProcessingException {
        return find.index(this).async();
    }

    public static Promise<Result<Person>> getById(String id) {
        return find.get(id).async();
    }

    public static Promise<Result<Person>> delete(String id) {
        return find.delete(id).async();
    }

    public static Promise<Result<Person>> count()  {
        return find.count(null).async();
    }

    public static List<Person> getAll() throws Exception {
        return find.search(null).get().contents;
    }

    public static Promise<Result<Person>> getAdults() {
        return find.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(18))).async();
    }


}
