import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

import java.util.List;
import java.util.Map;

@Type.Name("mydemotypettt")
public class DemoType extends Index {

    private String name;
    private int age;
    private List<String> things;
    private Map<String, Demo> map;

    public static Finder<DemoType> find = new Finder<>(DemoType.class);

    public DemoType(String name, int age, List<String> things, Map<String, Demo> map) {
        this.name = name;
        this.age = age;
        this.things = things;
        this.map = map;
    }

    public DemoType() {}

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

        public Demo(int value, List<Integer> ints) {
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


