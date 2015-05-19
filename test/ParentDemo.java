import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

@Type.Name(value="parentdemotesteoooii")
public class ParentDemo extends Index {

    public String location;
    public int number;

    public static Finder<ParentDemo> find = new Finder<>(ParentDemo.class);

    @JsonCreator
    public ParentDemo(@JsonProperty("location") String location, @JsonProperty("number") int number) {
        this.location = location;
        this.number = number;
    }

}
