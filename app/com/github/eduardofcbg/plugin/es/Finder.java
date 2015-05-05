package com.github.eduardofcbg.plugin.es;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.search.SearchHit;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Finder<T extends Model> {

    private Class<T> from;
    private Utils utils;

    public Finder(Class<T> from) {
        this.from = from;
        this.utils = new Utils();
    }

    public Utils utils() {
        return utils;
    }

    public IndexResponse index(T toIndex) throws Exception {
        IndexResponse response = getClient()
                .prepareIndex(getIndexName(), getTypeName())
                .setSource(ESPlugin.getPlugin().getMapper().writeValueAsBytes(toIndex))
                .execute()
                .actionGet();
        return response;
    }

    //todo must throw exception when item is not found, is the get method async?
    public DeleteResponse delete(String id) throws Exception {
        DeleteResponse response = getClient().prepareDelete(getIndexName(), getTypeName(), id)
                .execute()
                .actionGet();
        return response;
    }

    public T getAndParse(String id) throws NullPointerException {
        return  utils.toBean(
                getClient()
                        .prepareGet(getIndexName(), getTypeName(), id)
                        .execute()
                        .actionGet()
        );
    }

    public GetResponse get(String id) {
        return getClient()
                .prepareGet(getIndexName(), getTypeName(), id)
                .execute()
                .actionGet();
    }

    public UpdateResponse update(Supplier<T> toUpdate, Consumer<T> consumer) throws Exception {
        boolean done = false;
        UpdateResponse response = null;
        while(!done) {
            done = true;
            try {
                T willUpdate = toUpdate.get();
                consumer.accept(willUpdate);
                response = getClient().prepareUpdate(
                        getIndexName(), getTypeName(), willUpdate.getId())
                        .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(willUpdate))
                        .setVersion(willUpdate.getVersion())
                        .get();
            } catch (Exception e) { done = false; }
        }
        return response;
    }

    public UpdateBuilder updateIndex(String id) {
        return new UpdateBuilder(id);
    }

    public class UpdateBuilder {

        HashMap<String, Object> fields;
        String id;

        public UpdateBuilder(String id) {
            super();
            this.fields = new HashMap<>();
            this.id = id;
        }

        public UpdateBuilder field(String field, Object object) {
            fields.put(field, object);
            return this;
        }

        public UpdateResponse update() throws Exception {
            UpdateResponse response = getClient().prepareUpdate(getIndexName(), getTypeName(), id)
                    .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(fields))
                    .get();
            return response;
        }

    }

    public SearchResponse search(Consumer<SearchRequestBuilder> consumer) throws IOException {
        SearchRequestBuilder builder = getClient().prepareSearch(getIndexName()).setTypes(getTypeName());
        if (consumer != null) consumer.accept(builder);
        SearchResponse response = builder.execute().actionGet();
        if (response.isTimedOut() || response.getFailedShards() == response.getTotalShards()) throw new IOException();
        return response;
    }

    public List<T> searchAndParse(Consumer<SearchRequestBuilder> consumer) throws IOException {
        return utils().toBeans(search(consumer));
    }

    public CountResponse count(Consumer<CountRequestBuilder> consumer) throws Exception {
        CountRequestBuilder builder = getClient().prepareCount(getIndexName());
        if (consumer != null) consumer.accept(builder);
        CountResponse response = builder.execute().actionGet();
        if (response.getFailedShards() == response.getTotalShards()) throw new IOException();
        return builder.execute().actionGet();
    }

    public String getIndexName() {
        return from.getAnnotation(Entity.class).index();
    }

    public String getTypeName() {
        return from.getSimpleName().toLowerCase();
    }

    public static Client getClient() {
        return ESPlugin.getPlugin().getClient();
    }

    public class Utils {

        public List<T> toBeans(SearchResponse hits) {
            List<T> beans = new ArrayList<>();
            SearchHit[] newHits = hits.getHits().getHits();
            for(int i = 0; i < newHits.length; i++) {
                final SearchHit hit = newHits[i];
                T bean = ESPlugin.getPlugin().getMapper().convertValue(hit.getSource(), from);
                bean.setId(hit.getId());
                bean.setVersion(hit.getVersion());
                beans.add(bean);
            }
            return beans;
        }

        public T toBean(GetResponse result) {
            T bean = ESPlugin.getPlugin().getMapper().convertValue(result.getSource(), from);
            bean.setId(result.getId());
            bean.setVersion(result.getVersion());
            return bean;
        }

        public T toBean(GetResult result) {
            T bean = ESPlugin.getPlugin().getMapper().convertValue(result.getSource(), from);
            bean.setId(result.getId());
            bean.setVersion(result.getVersion());
            return bean;
        }


    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Entity {
        String index();
    }


}
