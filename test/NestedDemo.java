import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NestedDemo {

    public String content;
    public long stars;
    public List<String> responses;

    @JsonCreator
    public NestedDemo(@JsonProperty("content") String content, @JsonProperty("stars") long stars, @JsonProperty("responses") List<String> responses) {
        this.content = content;
        this.stars = stars;
        this.responses = responses;
    }

}
