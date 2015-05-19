import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

import java.util.List;
import java.util.Map;

@Type.Name(value="mydemotype")
public class DemoType extends Index {

    private String name;
    private int age;
    private List<String> things;
    private Map<String, Demo> map;

    public static Finder<DemoType> find = new Finder<>(DemoType.class);

    @JsonCreator
    public DemoType(@JsonProperty("name") String name, @JsonProperty("age") int age, @JsonProperty("things") List<String> things, @JsonProperty("map") Map<String, Demo> map) {
        this.name = name;
        this.age = age;
        this.things = things;
        this.map = map;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<String> getThings() {
        return things;
    }

    public void setThings(List<String> things) {
        this.things = things;
    }

    public Map<String, Demo> getMap() {
        return map;
    }

    public void setMap(Map<String, Demo> map) {
        this.map = map;
    }

    public static class Demo {

        private int value;
        private List<Integer> ints;

        @JsonCreator
        public Demo(@JsonProperty("value") int value, @JsonProperty("ints") List<Integer> ints) {
            this.value = value;
            this.ints = ints;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<Integer> getInts() {
            return ints;
        }

        public void setInts(List<Integer> ints) {
            this.ints = ints;
        }

        @Override
        public boolean equals(Object o) {
            Demo original = (Demo) o;
            return ((Demo) o).getValue() ==value && ints.equals(((Demo) o).getInts());
        }

    }

}


