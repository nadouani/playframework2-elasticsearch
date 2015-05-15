package com.github.eduardofcbg.plugin.es;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;
import scala.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Finder <T extends Index> {

    private Class<T> from;
    private static final ObjectMapper mapper = ESPlugin.getPlugin().getMapper();

    public Finder(Class<T> from) {
        this.from = from;
    }

    public Promise<IndexResponse> index(T toIndex) {
        IndexRequestBuilder builder = null;
        try {
            builder = getClient().prepareIndex(getIndexName(), getTypeName())
                    .setSource(mapper.writeValueAsBytes(toIndex));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        RedeemablePromise<IndexResponse> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                toIndex.setId(indexResponse.getId());
                toIndex.setVersion(indexResponse.getVersion());
                promise.success(indexResponse);
            }
            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable);
            }
        });
    	return F.Promise.wrap(promise.wrapped());
    }
    
    public Promise<T> get(String id) {
        GetRequestBuilder builder = getClient().prepareGet(getIndexName(), getTypeName(), id);

        RedeemablePromise<T> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                promise.success(parse(getResponse));
            }
            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable.getCause().getCause());
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }

    public Promise<DeleteResponse> delete(String id) {
        DeleteRequestBuilder builder = getClient().prepareDelete(getIndexName(), getTypeName(), id);

        RedeemablePromise<DeleteResponse> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<DeleteResponse>() {
            @Override
            public void onResponse(DeleteResponse deleteResponse) {
            	if (!deleteResponse.isFound()) 
            		promise.failure(new NullPointerException("No item found to be deleted."));
            	else promise.success(deleteResponse);
            }
            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable);
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }
        
    public Promise<Long> count(Consumer<CountRequestBuilder> consumer) {
        CountRequestBuilder builder = getClient().prepareCount(getIndexName());
        if (consumer != null) consumer.accept(builder);

        RedeemablePromise<Long> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<CountResponse>() {
            @Override
            public void onResponse(CountResponse countResponse) {
                promise.success(countResponse.getCount());
            }
            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable);
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }
    
    public Promise<List<T>> search(Consumer<SearchRequestBuilder> consumer) {
        SearchRequestBuilder builder = getClient().prepareSearch(getIndexName()).setTypes(getTypeName());
        if (consumer != null) consumer.accept(builder);

        RedeemablePromise<List<T>> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<SearchResponse>() {
            @Override
            public void onResponse(SearchResponse searchResponse) {
                promise.success(parse(searchResponse));
            }
            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable);
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }

    public UpdateBuilder update(String id) {
    	return new UpdateBuilder(id);
    }

    public Promise<UpdateResponse> update(Supplier<Promise<T>> supplier, Consumer<T> update, Redo<T> callback) throws JsonProcessingException {
        return new UpdateBuilder(null).supply(supplier, update, callback);
    }

    public Promise<UpdateResponse> update(Supplier<Promise<T>> supplier, Consumer<T> update) throws JsonProcessingException {
        return update(supplier, update, null);
    }

    public class UpdateBuilder {

        Map<String, Object> fields;
        T supplied;
        String id;

        public UpdateBuilder(String id) {
            this.fields = new HashMap<>();
            this.id = id;
        }

        public UpdateBuilder field(String field, Object object) {
            fields.put(field, object);
            return this;
        }

        private Promise<UpdateResponse> supply(Supplier<Promise<T>> supplier, Consumer<T> update, Redo<T> callback) throws JsonProcessingException {
        	this.supplied = supplier.get().get(10000);
        	this.id = supplied.getId().orElseThrow(IllegalArgumentException::new);
        	update.accept(supplied);
            Promise<UpdateResponse> result;
            if (callback != null)
                result = update(supplied.getVersion().orElseThrow(IllegalArgumentException::new));
            else result = update(null);
            result.onFailure(t -> {
                if (t.getClass().equals(IllegalAccessException.class)) {
                    if (callback != null)
                        callback.redo(get(id).get(10000), supplied);
                    supply(supplier, update, callback);
                } else {
                    throw t;
                }
            });
            return result;
        }

        public Promise<UpdateResponse> execute() throws JsonProcessingException {
            return update(null);
        }

        public Promise<UpdateResponse> execute(Long version, ManualRedo<T> callback) throws JsonProcessingException {
            if (version == null) throw new IllegalArgumentException("You must specify a version for safe execute");
            Promise<UpdateResponse> result = update(version);
            result.onFailure(t -> {
                if (t.getClass().equals(IllegalAccessException.class)) {
                    callback.redo(get(id).get(10000), fields);
                    execute(version, callback);
                } else {
                    throw t;
                }
            });
            return result;
        }

        private Promise<UpdateResponse> update(Long version) throws JsonProcessingException {
        	if (id == null) throw new IllegalArgumentException("An Id or a supplier must be specified");
            if (version == null) play.Logger.warn("Updating without specifying a supplier or version. This may cause cause concurrency problems.");

            Object updateObject;
        	if (supplied != null) updateObject = supplied;
        	else updateObject = fields;
        	
        	UpdateRequestBuilder builder = getClient().prepareUpdate(getIndexName(), getTypeName(), id)
                        .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(updateObject));
            if (version != null)
                builder.setVersion(version);

            RedeemablePromise<UpdateResponse> promise = RedeemablePromise.empty();
            builder.execute(new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse updateResponse) {
                    if (version != null && version != updateResponse.getVersion())
                            promise.failure(new IllegalAccessException("Tried to update outdated document."));
                    else
                        promise.success(updateResponse);
                }
                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable.getCause().getClass().equals(org.elasticsearch.index.engine.DocumentMissingException.class))
                        promise.failure(new NullPointerException("No item found to be updated."));
                    else promise.failure(throwable.getCause());
                }
            });
            return F.Promise.wrap(promise.wrapped());
        }
    }
    
    public String getIndexName() {
        return ESPlugin.getPlugin().indexName();
    }

    public String getTypeName() {
        return from.getAnnotation(Index.Type.class).name();
    }

    public Map<String, Object> parse(Object obj) {
        return getMapper().convertValue(obj, Map.class);
    }

    public List<T> parse(SearchResponse hits) {
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

    public T parse(GetResponse result) {
        T bean = ESPlugin.getPlugin().getMapper().convertValue(result.getSource(), from);
        bean.setId(result.getId());
        bean.setVersion(result.getVersion());
        return bean;
    }

    public static Client getClient() {
        return ESPlugin.getPlugin().getClient();
    }
    public static ObjectMapper getMapper() { return mapper; }

}
