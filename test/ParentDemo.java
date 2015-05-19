import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

import java.util.Arrays;

@Type.Name("myparentdemottt")
public class ParentDemo extends Index {

    public String location;
    public int number;
    @Type.NestedField
    public NestedDemo nested;

    public static Finder<ParentDemo> find = new Finder<>(ParentDemo.class);

    @JsonCreator
    public ParentDemo(@JsonProperty("location") String location, @JsonProperty("number") int number, @JsonProperty("comment") NestedDemo nested) {
        this.location = location;
        this.number = number;
        this.nested = new NestedDemo("ola", 55, Arrays.asList("ola!", "ole!", "..."));
    }

    public void setNested(NestedDemo nested) {
        this.nested = nested;
    }

}
