import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.eduardofcbg.plugin.es.ManualRedo;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.junit.Test;

import play.test.*;
import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;

public class PluginTest {

    public static long timeOut = 1000;

    public FakeApplication esFakeApplication() {
        Map<String, Object> config = new HashMap<>();
        config.put("es.client", "127.0.0.1:9300");
        config.put("es.enabled", true);

        List<String> additionalPlugin = new ArrayList<>();
        additionalPlugin.add("com.github.eduardofcbg.plugin.es.ESPlugin");
        return fakeApplication(config, additionalPlugin);
    }

    public DemoIndex demoFactory() {
        return new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
    }

    @Test
    public void indexName() {
        running(esFakeApplication(), () -> {
            assertThat(DemoIndex.finder.getIndexName()).isEqualTo("play-es");
        });
    }

    @Test
    public void indexType() {
        running(esFakeApplication(), () -> {
            assertThat(DemoIndex.finder.getTypeName()).isEqualTo("demoindex");
        });
    }

    @Test
    public void indexAndGet() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = demoFactory();
            
            assertThat(demo.getVersion().isPresent()).isFalse();
            assertThat(demo.getId().isPresent()).isFalse();
            
            IndexResponse response = DemoIndex.finder.index(demo).get(timeOut);
            assertThat(response).isNotNull();
            assertThat(response.getIndex()).isEqualTo("play-es");
            assertThat(response.getType()).isEqualTo("demoindex");
            assertThat(response.isCreated()).isTrue();

            String id = response.getId();
            assertThat(id).isNotNull();

            DemoIndex got = DemoIndex.finder.get(id).get(timeOut);
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
            DemoIndex demo = demoFactory();
            IndexResponse responseIndex = DemoIndex.finder.index(demo).get(timeOut);
            
            assertThat(responseIndex).isNotNull();
            String addedId = responseIndex.getId();

            DeleteResponse response = DemoIndex.finder.delete(addedId).get(timeOut);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(addedId);
        });
    }

    @Test(expected = NullPointerException.class)
    public void getNonExisting() throws Exception{
        running(esFakeApplication(), () -> {
            DemoIndex.finder.get("1110").get(timeOut);        	
        });
    }
    
    @Test(expected = NullPointerException.class)
    public void deleteNonExistent() {
        running(esFakeApplication(), () -> {
        	DemoIndex.finder.delete("1110").get(timeOut);
        });
    }

    @Test(expected = NullPointerException.class)
    public void updateNonExistent() {
        running(esFakeApplication(), () -> {
            try {
                DemoIndex.finder.update("12912", o -> {
                    o.setAge(11);
                    return o;
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void update() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = demoFactory();
            IndexResponse response = DemoIndex.finder.index(demo).get(timeOut);

            String id = response.getId();

            UpdateResponse updateResponse = null;
            try {
                updateResponse = DemoIndex.finder.update(id, u -> {
                    u.setName("Ben M.");
                    return u;
                }).get(timeOut);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(updateResponse).isNotNull();

            DemoIndex updated = DemoIndex.finder.get(id).get(timeOut);

            assertThat(updated.getName()).isEqualTo("Ben M.");

            assertThat(updated.getAge()).isEqualTo(demo.getAge());
            assertThat(updated.getThings()).isEqualTo(demo.getThings());
            assertThat(updated.getMap()).isEqualTo(demo.getMap());
        });
    }

    @Test
    public void updateWithConcurrency() {
        running(esFakeApplication(), () -> {
            DemoIndex demo =  demoFactory();
            IndexResponse response = DemoIndex.finder.index(demo).get(timeOut);

            String id = response.getId();
            System.out.println(id);

            play.Logger.warn(id);

            try {
                DemoIndex.finder.update(id, d -> {
                    d.setAge(10);
                    return d;
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            try {
                DemoIndex.finder.update(id, d -> {
                    d.setAge(11);
                    return d;
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoIndex.finder.get(id).get(timeOut).getVersion().get()).isEqualTo(3);

            try {
                DemoIndex.finder.update(id, d -> {
                    d.setAge(12);
                    return d;
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoIndex.finder.get(id).get(timeOut).getAge()).isEqualTo(12);

            DemoIndex original = DemoIndex.finder.get(id).get(timeOut);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            try {
                DemoIndex.finder.update(id, 1, d -> {
                    d.setAge(13);
                    return d;
                });
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            assertThat(DemoIndex.finder.get(id).get(timeOut).getAge()).isEqualTo(13);

        });
    }

    @Test
    public void extremeTest() {
        running(esFakeApplication(), () -> {
            int toAdd = 1000;
            DemoIndex demo = demoFactory();
            demo.setAge(0);
            String id = DemoIndex.finder.index(demo).get(timeOut).getId();
            play.Logger.warn("The id updated: " + id);
            for (int i = 0; i < toAdd; i++) {
                try {
                    DemoIndex.finder.update(demo, o -> {
                        o.setAge(o.getAge() + 1);
                        return o;
                    });
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(toAdd*10 + 10000);
            } catch (InterruptedException e) {
            }

            assertThat(DemoIndex.finder.get(id).get(timeOut).getAge()).isEqualTo(toAdd);
        });
    }

    @Test
    public void search() {
        running(esFakeApplication(), () -> {
            DemoIndex demo =  demoFactory();
            DemoIndex.finder.index(demo);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            List<DemoIndex> list = DemoIndex.finder.search(null).get(timeOut);

            assertThat(list).isNotNull();
            assertThat(list.size()).isNotEqualTo(0);
            DemoIndex element = list.get(0);
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
            DemoIndex demo1 =  demoFactory();
            DemoIndex demo2 = demoFactory();
            demo1.setAge(35);
            demo2.setAge(35);
            DemoIndex.finder.index(demo1);
            DemoIndex.finder.index(demo2);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            List<DemoIndex> list = DemoIndex.finder.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(30))).get(timeOut);

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
            List<DemoIndex> list = DemoIndex.finder.search(s -> s.setPostFilter(FilterBuilders.rangeFilter("age").from(10000000))).get(timeOut);

            assertThat(list).isNotNull();
            assertThat(list.size()).isEqualTo(0);
        });
    }

    @Test
    public void count() {
        running(esFakeApplication(), () -> {
            DemoIndex.finder.index(demoFactory());

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}

            long count = DemoIndex.finder.count(null).get(timeOut);
            assertThat(count).isGreaterThan(0);
        });
    }


}