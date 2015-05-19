import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

import java.util.List;

@Type.Name(value="mychilddemo")
@Type.Parent(value="myparentdemo")
public class ChildDemo extends Index {

    public List<Integer> numbers;
    public String value;

    public static Finder<ChildDemo> find = new Finder<>(ChildDemo.class);

    @JsonCreator
    public ChildDemo(@JsonProperty("numbers") List<Integer> numbers, @JsonProperty("value") String value) {
        this.numbers = numbers;
        this.value = value;
    }

}
