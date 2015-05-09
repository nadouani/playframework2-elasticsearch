import java.util.*;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;

import play.test.*;
import static play.test.Helpers.*;
import static org.fest.assertions.Assertions.*;


public class PluginTest {

    public FakeApplication esFakeApplication() {
        Map<String, Object> config = new HashMap<>();
        config.put("es.host", "127.0.0.1");
        config.put("es.status", "enabled");

        List<String> additionalPlugin = new ArrayList<>();
        additionalPlugin.add("com.github.eduardofcbg.plugin.es.ESPlugin");
        return fakeApplication(config, additionalPlugin);
    }

    @org.junit.Test
    public void indexName() {
        DemoIndex demo = new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
        assertThat(DemoIndex.finder.getIndexName()).isEqualTo("testindex1");
    }

    @org.junit.Test
    public void indexType() {
        DemoIndex demo = new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
        assertThat(DemoIndex.finder.getTypeName()).isEqualTo("demoindex");
    }

    @org.junit.Test
    public void index() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                    Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
            IndexResponse response = null;
            boolean ex = false;
            try {
                response = DemoIndex.finder.index(demo);
            } catch (Exception e) {
                ex = true;
            }
            assertThat(response).isNotNull();
            assertThat(ex).isFalse();
            assertThat(response.getIndex()).isEqualTo("testindex1");
            assertThat(response.getType()).isEqualTo("demoindex");
            assertThat(response.isCreated()).isTrue();

            String id = response.getId();
            assertThat(id).isNotNull();
            DemoIndex got = null;
            try {
                got = DemoIndex.finder.getAndParse(id);
            } catch (Exception e) {
            }
            assertThat(got).isNotNull();

            assertThat(got.getId()).isEqualTo(id);
            assertThat(got.getTimestamp()).isEqualTo(demo.getTimestamp());
            assertThat(got.getName()).isEqualTo(demo.getName());
            assertThat(got.getAge()).isEqualTo(demo.getAge());
            assertThat(got.getThings()).isEqualTo(demo.getThings());
            assertThat(got.getMap()).isEqualTo(demo.getMap());

            assertThat(got.getVersion()).isEqualTo(1);
            assertThat(demo.getVersion()).isNull();
        });
    }

    @org.junit.Test
    public void delete() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                    Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
            IndexResponse responseIndex = null;
            boolean exIndex = false;
            try {
                responseIndex = DemoIndex.finder.index(demo);
            } catch (Exception e) {
                exIndex = true;
            }
            assertThat(responseIndex).isNotNull();
            assertThat(exIndex).isFalse();

            String addedId = responseIndex.getId();

            DeleteResponse response = null;
            boolean exc = false;
            try {
                response = DemoIndex.finder.delete(addedId);
            } catch (Exception e) {
                exc = true;
            }
            assertThat(exc).isFalse();
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(addedId);
            DemoIndex got = null;
            boolean ex = false;
            try {
                try {
					got = DemoIndex.finder.getAndParse(addedId);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } catch (NullPointerException e) {
                ex = true;
            }
            assertThat(got).isNull();
            assertThat(ex).isTrue();
        });
    }

    //todo
    @org.junit.Test
    public void deleteNonExistent() {
        running(esFakeApplication(), () -> {
            DeleteResponse response = null;
            boolean ex = false;
            try {
                response = DemoIndex.finder.delete("111");
            } catch (Exception e) {
                ex = true;
            }
        });
        
    }

    @org.junit.Test
    public void updateWithSupplier() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = new DemoIndex("Ben M.", 20, Arrays.asList("element1", "element2"),
                    Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
            IndexResponse response = null;
            try {
                response = DemoIndex.finder.index(demo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();

            String id = response.getId();
            UpdateResponse update = null;

            try {
				update = DemoIndex.finder.update(() -> {
					try {
						return DemoIndex.finder.getAndParse(id);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return null;
				}, original -> original.setName("Ben"));
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

            assertThat(update).isNotNull();

            DemoIndex updated = null;
			try {
				updated = DemoIndex.finder.getAndParse(id);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            assertThat(updated.getName()).isEqualTo("Ben");

            assertThat(updated.getAge()).isEqualTo(demo.getAge());
            assertThat(updated.getThings()).isEqualTo(demo.getThings());
            assertThat(updated.getMap()).isEqualTo(demo.getMap());

        });
    }

    @org.junit.Test
    public void updateWithId() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = new DemoIndex("Ben M.", 20, Arrays.asList("element1", "element2"),
                    Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
            IndexResponse response = null;
            try {
                response = DemoIndex.finder.index(demo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();

            String id = response.getId();
            UpdateResponse update = null;

            try {
                update = DemoIndex.finder.updateIndex(id).field("name", "Ben").update();
            } catch (Exception e) {
                e.printStackTrace();
            }

            assertThat(update).isNotNull();

            DemoIndex updated = null;
			try {
				updated = DemoIndex.finder.getAndParse(id);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            assertThat(updated.getName()).isEqualTo("Ben");

            assertThat(updated.getAge()).isEqualTo(demo.getAge());
            assertThat(updated.getThings()).isEqualTo(demo.getThings());
            assertThat(updated.getMap()).isEqualTo(demo.getMap());

        });

    }

    //todo include a little more complicated search
    @org.junit.Test
    public void search() {
        running(esFakeApplication(), () -> {
            DemoIndex demo = new DemoIndex("Ben", 20, Arrays.asList("element1", "element2"),
                    Collections.singletonMap("key1", new DemoIndex.Demo(66, Arrays.asList(12, 15))));
            IndexResponse response = null;
            try {
                response = DemoIndex.finder.index(demo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();

            List<DemoIndex> list = null;
            try {
				list = DemoIndex.finder.utils().toBeans(DemoIndex.finder.search(null));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

    //todo: would be better we cloud make this test a little better
    @org.junit.Test
    public void count() {
        running(esFakeApplication(), () -> {
            long count = 0;
            try {
                count = DemoIndex.finder.count(null).getCount();
            } catch (Exception e) {
                e.printStackTrace();
            }
            assertThat(count).isGreaterThan(0);
        });
    }


}
