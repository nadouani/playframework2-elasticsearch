import com.github.eduardofcbg.plugin.es.ES;
import com.github.eduardofcbg.plugin.es.ESModule;
import org.junit.Test;
import play.Environment;
import play.Mode;
import play.inject.Injector;
import play.inject.guice.GuiceApplicationBuilder;

import java.util.Arrays;
import java.util.Collections;

public class PluginTest {

    private final static long timeOut = 1000;

    private Injector esFakeApplication() {
        return new GuiceApplicationBuilder()
                .in(Environment.simple())
                .in(Mode.TEST)
                .load(new ESModule())
                .injector();
    }

    private DemoType demoFactory() {
        return new DemoType("Ben", 20, Arrays.asList("element1", "element2"),
                Collections.singletonMap("key1", new DemoType.Demo(66, Arrays.asList(12, 15))));
    }

    @Test
    public void indexName() {
            ES es = esFakeApplication().instanceOf(ES.class);
            play.Logger.warn(es.indexName());
            //assertThat(1).isNotNull();

            //DemoType demo = demoFactory();

            //assertThat(demo).isNotNull();
            //assertThat(demo.find).isNotNull();

            //assertThat(DemoType.find).isNotNull();
            //assertThat(DemoType.find.component).isNotNull();
        //assertThat(DemoType.find.getClient()).isNotNull();
        //  assertThat(DemoType.find.getIndex()).isEqualTo("play-ess");
    }

/*
    @Test
    public void indexType() {
        running(esFakeApplication(), () -> {
            assertThat(DemoType.find.getType()).isEqualTo("mydemotypettt");
        });
    }

    @Test
    public void indexAndGet() {
        running(esFakeApplication(), () -> {
            DemoType demo = demoFactory();
            
            assertThat(demo.getVersion().isPresent()).isFalse();
            assertThat(demo.getId().isPresent()).isFalse();
            
            IndexResponse response = DemoType.find.index(demo).get(timeOut);
            assertThat(response).isNotNull();
            //assertThat(response.getIndex()).isEqualTo("play-es");
            //assertThat(response.getType()).isEqualTo("demoindex");
            assertThat(response.isCreated()).isTrue();

            String id = response.getId();
            assertThat(id).isNotNull();

            DemoType got = DemoType.find.get(id).get(timeOut);
            assertThat(got).isNotNull();

            assertThat(got.getId().get()).isEqualTo(id);
            assertThat(got.getTimestamp()).isEqualTo(demo.getTimestamp());
            assertThat(got.getName()).isEqualTo(demo.getName());
            assertThat(got.getAge()).isEqualTo(demo.getAge());
            assertThat(got.getThings()).isEqualTo(demo.getThings());
            assertThat(got.getMap()).isEqualTo(demo.getMap());

            assertThat(got.getVersion().get()).isEqualTo(demo.getVersion().get()).isEqualTo(1);
        });
    }

    @Test
    public void delete() {
        running(esFakeApplication(), () -> {
            DemoType demo = demoFactory();
            IndexResponse responseIndex = DemoType.find.index(demo).get(timeOut);
            
            assertThat(responseIndex).isNotNull();
            String addedId = responseIndex.getId();

            DeleteResponse response = DemoType.find.delete(addedId).get(timeOut);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(addedId);
        });
    }

    @Test(expected = NullPointerException.class)
    public void getNonExisting() throws Exception{
        running(esFakeApplication(), () -> {
            DemoType.find.get("1110").get(timeOut);
        });
    }
    
    @Test(expected = NullPointerException.class)
    public void deleteNonExistent() {
        running(esFakeApplication(), () -> {
        	DemoType.find.delete("1110").get(timeOut);
        });
    }

    @Test(expected = NullPointerException.class)
    public void updateNonExistent() {
        running(esFakeApplication(), () -> {
            DemoType.find.update("12912", o -> {
                o.setAge(11);
                return o;
            });
        });
    }

    @Test
    public void update() {
        running(esFakeApplication(), () -> {
            DemoType demo = demoFactory();
            IndexResponse response = DemoType.find.index(demo).get(timeOut);

            String id = response.getId();

            UpdateResponse updateResponse = null;
            updateResponse = DemoType.find.update(id, u -> {
                u.setName("Ben M.");
                return u;
            }).get(timeOut);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(updateResponse).isNotNull();

            DemoType updated = DemoType.find.get(id).get(timeOut);

            assertThat(updated.getName()).isEqualTo("Ben M.");

            assertThat(updated.getAge()).isEqualTo(demo.getAge());
            assertThat(updated.getThings()).isEqualTo(demo.getThings());
            assertThat(updated.getMap()).isEqualTo(demo.getMap());
        });
    }

    @Test
    public void updateWithConcurrency() {
        running(esFakeApplication(), () -> {
            DemoType demo =  demoFactory();
            IndexResponse response = DemoType.find.index(demo).get(timeOut);

            String id = response.getId();
            System.out.println(id);

            play.Logger.warn(id);

            DemoType.find.update(id, d -> {
                d.setAge(10);
                return d;
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            DemoType.find.update(id, d -> {
                d.setAge(11);
                return d;
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoType.find.get(id).get(timeOut).getVersion().get()).isEqualTo(3);

            DemoType.find.update(id, d -> {
                d.setAge(12);
                return d;
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoType.find.get(id).get(timeOut).getAge()).isEqualTo(12);

            DemoType original = DemoType.find.get(id).get(timeOut);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            DemoType.find.update(id, 1, d -> {
                d.setAge(13);
                return d;
            });

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoType.find.get(id).get(timeOut).getAge()).isEqualTo(13);

        });
    }

    @Test
    public void extremeTest() {
        running(esFakeApplication(), () -> {
            int toAdd = 100;
            DemoType demo = demoFactory();
            demo.setAge(0);
            String id = DemoType.find.index(demo).get(timeOut).getId();
            play.Logger.warn("The id updated: " + id);
            for (int i = 0; i < toAdd; i++) {
                DemoType.find.update(demo, o -> {
                    o.setAge(o.getAge() + 1);
                    return o;
                });

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(toAdd * 10 + 10000);
            } catch (InterruptedException e) {
            }

            assertThat(DemoType.find.get(id).get(timeOut).getAge()).isEqualTo(toAdd);
        });
    }

    @Test
    public void search() {
        running(esFakeApplication(), () -> {
            DemoType demo =  demoFactory();
            DemoType.find.index(demo);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            List<DemoType> list = DemoType.find.search(s ->
                    s.setQuery(QueryBuilders.matchQuery("_id", demo.getId().get())))
            .get(timeOut);

            assertThat(list).isNotNull();
            assertThat(list.size()).isEqualTo(1);
            DemoType element = list.get(0);
            assertThat(element).isNotNull();

            assertThat(element.getName()).isEqualTo(demo.getName());
            assertThat(element.getAge()).isEqualTo(demo.getAge());
            assertThat(element.getThings()).isEqualTo(demo.getThings());
            assertThat(element.getMap()).isEqualTo(demo.getMap());
        });
    }

    @Test
    public void advancedSearch() {
        running(esFakeApplication(), () -> {
            DemoType demo1 =  demoFactory();
            DemoType demo2 = demoFactory();
            demo1.setAge(35);
            demo2.setAge(35);
            DemoType.find.index(demo1);
            DemoType.find.index(demo2);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            List<DemoType> list = DemoType.find.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(30).to(40))).get(timeOut);

            assertThat(list).isNotNull();
            assertThat(list.size()).isGreaterThanOrEqualTo(2);

            list.forEach(element -> {
                assertThat(element.getName()).isEqualTo(demo1.getName());
                assertThat(element.getAge()).isEqualTo(demo1.getAge());
                assertThat(element.getThings()).isEqualTo(demo1.getThings());
                assertThat(element.getMap()).isEqualTo(demo1.getMap());
            });

        });
    }

    @Test
    public void searchEmptyResults() {
        running(esFakeApplication(), () -> {
            List<DemoType> list = DemoType.find.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(10000000))).get(timeOut);

            assertThat(list).isNotNull();
            assertThat(list.size()).isEqualTo(0);
        });
    }

    @Test
    public void count() {
        running(esFakeApplication(), () -> {
            DemoType.find.index(demoFactory());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            long count = DemoType.find.count(null).get(timeOut);
            assertThat(count).isGreaterThan(0);
        });
    }

    @Test
    public void indexParent() {
        running(esFakeApplication(), () -> {
            ParentDemo parent = new ParentDemo("London", 2015, null);
            IndexResponse indexResponse = ParentDemo.find.index(parent).get(timeOut);

            play.Logger.warn("thiefw: " + indexResponse.getId());

            assertThat(indexResponse).isNotNull();
            assertThat(indexResponse.getId()).isNotEmpty();

            ParentDemo got = ParentDemo.find.get(indexResponse.getId()).get(timeOut);

            assertThat(got.location).isEqualTo(parent.location);
            assertThat(got.number).isEqualTo(parent.number);
            assertThat(got.getId().get()).isEqualTo(parent.getId().get()).isEqualTo(indexResponse.getId());
        });
    }

    @Test
    public void indexChild() {
        //use the childs's Finder helper to index and get it's child
        running(esFakeApplication(), () -> {
            ParentDemo parent = new ParentDemo("London", 2015, null);
            IndexResponse parentIndex = ParentDemo.find.index(parent).get(timeOut);

            assertThat(parentIndex.getId()).isEqualTo(parent.getId().get());

            ChildDemo child = new ChildDemo(Arrays.asList(20, 19, 12), "any_value");
            IndexResponse indexResponse = ChildDemo.find.indexChild(child, parent.getId().get()).get(timeOut);

            assertThat(indexResponse).isNotNull();
            assertThat(indexResponse.getId()).isEqualTo(child.getId().get());

            ChildDemo got = ChildDemo.find.getChild(indexResponse.getId(), parent.getId().get()).get(timeOut);

            assertThat(got.numbers).isEqualTo(child.numbers);
            assertThat(got.value).isEqualTo(child.value);
            assertThat(got.getId().get()).isEqualTo(child.getId().get()).isEqualTo(indexResponse.getId());
        });
    }

    @Test
    public void parent() {
        running(esFakeApplication(), () -> {
            ParentDemo parent = new ParentDemo("London", 2015, null);
            IndexResponse indexResponse = ParentDemo.find.index(parent).get(timeOut);

            assertThat(indexResponse).isNotNull();
            assertThat(indexResponse.getId()).isEqualTo(parent.getId().get());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            ChildDemo child = new ChildDemo(Arrays.asList(35, 78), "ola!");
            IndexResponse childIndex = ChildDemo.find.indexChild(child, parent.getId().get()).get(timeOut);

            assertThat(child.getId().get()).isEqualTo(childIndex.getId());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            List<ChildDemo> results = ChildDemo.find.getAsChildrenOf(ParentDemo.class, parent.getId().get()).get(timeOut);

            assertThat(results.size()).isEqualTo(1);
            assertThat(results.size()).isEqualTo(1);

            ChildDemo indexedChild = results.get(0);

            assertThat(indexedChild.getTimestamp()).isEqualTo(child.getTimestamp());
            assertThat(indexedChild.numbers).isEqualTo(child.numbers);
            assertThat(indexedChild.value).isEqualTo(child.value);

        });
    }
*/


}