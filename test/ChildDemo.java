import com.github.eduardofcbg.plugin.es.Finder;
import com.github.eduardofcbg.plugin.es.Index;
import com.github.eduardofcbg.plugin.es.Type;

import java.util.List;

@Type.Name("mychilddemottt")
@Type.Parent("myparentdemottt")
public class ChildDemo extends Index {

    public List<Integer> numbers;
    public String value;

    public static Finder<ChildDemo> find = new Finder<>(ChildDemo.class);

    public ChildDemo(List<Integer> numbers, String value) {
        this.numbers = numbers;
        this.value = value;
    }

    public ChildDemo(){}

}
