package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
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
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Helper class that queries your elasticsearch cluster. Should be instantiated statically in your models, meaning
 * that is is associated with each ES type.
 * @param <T> The type of your model
 */
public class Finder <T extends Index> {

    private Class<T> from;
    private static ObjectMapper mapper = ESPlugin.getPlugin().getMapper();

    public Finder(Class<T> from) {
        this.from = from;
    }

    /**
     * Indexes a model as a document in the cluster.
     * The model object indexed will then have associated with it a version and an id.
     * @param toIndex The model to be indexed
     * @return A promise (async) of the response given by the server
     * @throws JsonProcessingException
     */
    public Promise<IndexResponse> index(T toIndex) throws JsonProcessingException {
        IndexRequestBuilder builder = getClient().prepareIndex(getIndexName(), getTypeName())
                    .setSource(mapper.writeValueAsBytes(toIndex));

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
                promise.failure(throwable.getCause());
            }
        });
    	return F.Promise.wrap(promise.wrapped());
    }

    /**
     * Gets a model from the elasticsearch cluster
     * @param id The unique id of the document that exists in the cluster
     * @return A promise (async) of the response given by the server
     * @throws java.lang.NullPointerException When the document is not found
     */
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

    /**
     * Deletes a document from elasticsearch
     * @param id If of the document associated with the model object you want to delete
     * @return A promise (async) of the response given by the server
     * @throws java.lang.NullPointerException When a document to delete is not found
     */
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
                promise.failure(throwable.getCause());
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }

    /**
     * Counts the number of occurrences of a specified querie.
     * @param consumer Pass a method to change the CountRequestBuilder via side effects
     * @return A a promise (async) of the number of existent results. If no consumer is specified (null)
     * the result will be the total number of indexed models of the finder's type
     */
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

    /**
     * Search for results using the default elasticsearch API.
     * @param consumer Pass a method to change the default SearchRequestBuilder via side effects for
     *                 more customized searches
     * @return If no consumer is specified, returns the promise (async) of a list containing all indexed documents
     * of the model's type.
     */
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

//    public Promise<UpdateResponse> update(Supplier<Promise<T>> supplier, Consumer<T> update, Redo<T> callback) throws JsonProcessingException {
//        return new UpdateBuilder(null).supply(supplier, update, callback);
//    }

//    public Promise<UpdateResponse> update(Supplier<Promise<T>> supplier, Consumer<T> update) throws JsonProcessingException {
//        return update(supplier, update, null);
//    }

    /**
     * Helper that enables the creation of update queries without having to create a
     * Hash<String, Object> manually.
     */
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

//        private Promise<UpdateResponse> supply(Supplier<Promise<T>> supplier, Consumer<T> update, Redo<T> callback) throws JsonProcessingException {
//        	this.supplied = supplier.get().get(10000);
//        	this.id = supplied.getId().orElseThrow(IllegalArgumentException::new);
//        	update.accept(supplied);
//            Promise<UpdateResponse> result;
//            if (callback != null)
//                result = update(supplied.getVersion().orElseThrow(IllegalArgumentException::new));
//            else result = update(null);
//            result.onFailure(t -> {
//                if (t.getClass().equals(IllegalAccessException.class)) {
//                    if (callback != null)
//                        callback.redo(get(id).get(10000), supplied);
//                    supply(supplier, update, callback);
//                } else {
//                    throw t;
//                }
//            });
//            return result;
//        }

        /**
         * Execute an update of constructed query discarding any problems related with concurrency.
         * Logs a warning in console.
         * @return A promise (async) of the response given by the server.
         * @throws JsonProcessingException
         */
        public Promise<UpdateResponse> execute() throws JsonProcessingException {
            return update(null);
        }

        /**
         * Execute and update of constructed query dealing with concurrency problems.
         * @param version A version of the document you except to update.
         * @param callback To deal with concurrency problems. If the excepted version to be updated is not the same,
         *                 this callback passed will be called. This method passed takes to parameters: The new current object stored
         *                 in the cluster, and a HashMap with which fields to update the current document. This map is the same that the one
         *                 that was created using the field() method while construction the query, and it should be changed via side effects.
         *                 This means that the callback is just a way to specify a second update when the first one fails.
         * @return A promise (async) of the response given by the server.
         * @throws JsonProcessingException
         */
        public Promise<UpdateResponse> execute(long version, ManualRedo<T> callback) throws JsonProcessingException {
            Promise<UpdateResponse> result = update(version);
            result.onFailure(t -> {
                if (t.getClass().equals(IllegalAccessException.class)) {
                    T actual = get(id).get(10000);
                    //in case there are concurrency problems, the fields to be updated should be changed
                    //via side effects with the callback specified.
                    callback.redo(actual, fields);
                    //after the problems are resolved by the user, the request for updating is done again
                    //using new data
                    execute(actual.getVersion().get(), callback);
                } else {
                    throw t;
                }
            });
            return result;
        }

        /**
         * Builds the actual request that is going to be sent to the server.
         * @param version The excepted version of the document on the cluster to be updated.
         * @return A promise (async) of the update response sent by the server.
         * @throws JsonProcessingException
         */
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
                    promise.success(updateResponse);
                }
                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable.getCause().getClass().equals(org.elasticsearch.index.engine.DocumentMissingException.class))
                        promise.failure(new NullPointerException("No item found to be updated."));
                    else {
                        if (throwable.getCause().getClass().equals(org.elasticsearch.index.engine.VersionConflictEngineException.class)) {
                            promise.failure(new IllegalAccessException("Tried to update outdated document."));
                            play.Logger.warn("yes....!");
                        }
                        else promise.failure(throwable.getCause());
                    }
                }
            });
            return F.Promise.wrap(promise.wrapped());
        }
    }

    /**
     * @return The name of the index (associated with the elasticsearch cluster).
     * This can be configured using your application.conf using the key "es.index" (see documentation on gihub repository).
     */
    public String getIndexName() {
        return ESPlugin.getPlugin().indexName();
    }

    /**
     * The name of the type of the model's finder.
     * @return Simply the annotated type name on the model
     */
    public String getTypeName() {
        return from.getAnnotation(Index.Type.class).name();
    }

    /**
     * Additional method for parsing objects
     * @param obj
     * @return
     */
    public Map<String, Object> parse(Object obj) {
        return getMapper().convertValue(obj, Map.class);
    }

    /**
     * Additional method for parsing objects
     * @param hits The response from the cluster.
     * @return The list of models that were queried from the ES server.
     */
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

    /**
     * Additional method for parsing a response
     * @param result A response from a Get resquest from the ES server.
     * @return
     */
    public T parse(GetResponse result) {
        T bean = ESPlugin.getPlugin().getMapper().convertValue(result.getSource(), from);
        bean.setId(result.getId());
        bean.setVersion(result.getVersion());
        return bean;
    }

    /**
     * Useful for more customized queries
     * @return The client from the official ES API.
     */
    public static Client getClient() {
        return ESPlugin.getPlugin().getClient();
    }

    /**
     * @return The jackson's Object Mapper that is used to parse the responses and indexing
     */
    public static ObjectMapper getMapper() { return mapper; }

}
