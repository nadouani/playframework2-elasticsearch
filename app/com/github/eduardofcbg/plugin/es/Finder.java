package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
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
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.search.SearchHit;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;


/**
 * Helper class that queries your elasticsearch cluster. Should be instantiated statically in your models, meaning
 * that is is associated with each ES type.
 * @param <T> The type of your model
 */
public class Finder <T extends Index> {

    private Class<T> from;
    private static ObjectMapper mapper = ESPlugin.getPlugin().getMapper();

    /**
     * Creates a finder and adds a custom mapping for the type associated with it.
     * @param from Types's class or the class that instantiates this helper.
     * @param consumer A consumer to construct the mapping via side effects.
     */
    public Finder(Class<T> from, Consumer<XContentBuilder> consumer) {
        this.from = from;
        try {
            XContentBuilder builder = jsonBuilder().startObject();
            builder.startObject(getTypeName());
            consumer.accept(builder);
            builder.endObject();
            builder.endObject();
            setMapping(builder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a finder for querying ES cluster
     * @param from Types's class or the class that instanciates this helper.
     */
    public Finder(Class<T> from) {
        this.from = from;
    }

    /**
     * Indexes a model as a document in the cluster.
     * The model object indexed will then have associated with it a version and an id.
     * @param toIndex The model to be indexed
     * @return A promise (async) of the response given by the server
     */
    public Promise<IndexResponse> index(T toIndex) {
        IndexRequestBuilder builder = null;
        try {
            builder = getClient().prepareIndex(getIndexName(), getTypeName())
                        .setSource(mapper.writeValueAsBytes(toIndex));
        } catch (JsonProcessingException e) { e.printStackTrace();}

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

    /**
     * Updated a document with the provided id. Before the update there will be made a get to get the current
     * state of the document
     * @param id The id of the document to update
     * @param version The excepted version of the document to update
     * @param change The function that can be passed that takes the original document and returns the one to be saved
     * @return A promise (async) of the response given by ES server.
     * @throws java.lang.NullPointerException When a document to update is not found
     */
    public Promise<UpdateResponse> update(String id, long version, Function<T, T> change) {
        T original = get(id).get(10000);
        original.setVersion(version);
        return update(original, original.getId().get(), change);
    }

    /**
     * Updated a document with the provided id. Before the update there will be made a get to get the current
     * state of the document.
     * @param id The id of the document to update
     * @param change The function that can be passed that takes the original document and returns the one to be saved
     * @return The function that can be passed that takes the original document and returns the one to be saved
     * @throws java.lang.NullPointerException When a document to update is not found
     */
    public Promise<UpdateResponse> update(String id, Function<T, T> change) {
        T original = get(id).get(10000);
        return update(original, original.getId().get(), change);
    }

    /**
     * Update a document given the java queried java object. No get request will be made because it's assumed
     * that the passed object already contains an id, or because it was indexed before or was queried from the db.
     * @param original The queried java object to find the associated ES document
     * @param change The function that can be passed that takes the original document and returns the one to be saved
     * @return The function that can be passed that takes the original document and returns the one to be saved
     * @throws java.lang.NullPointerException When a document to update is not found
     */
    public Promise<UpdateResponse> update(T original, Function<T, T> change) {
        return update(original, original.getId().get(), change);
    }

    /**
     * Update a document with a specified id and a specified original state for the object
     * @param original The original java object which state will be changed
     * @param change The function that can be passed that takes the original document and returns the one to be saved
     * @return The function that can be passed that takes the original document and returns the one to be saved
     * @throws java.lang.NullPointerException When a document to update is not found
     */
    public Promise<UpdateResponse> update(T original, String id, Function<T, T> change) {
        T toUpdate = change.apply(original);

        UpdateRequestBuilder builder;
        RedeemablePromise<UpdateResponse> promise = RedeemablePromise.empty();
        try {
            builder = getClient().prepareUpdate(getIndexName(), getTypeName(), id)
                    .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(toUpdate))
                    .setVersion(original.getVersion().get());

            builder.execute(new ActionListener<UpdateResponse>() {
                @Override
                public void onResponse(UpdateResponse updateResponse) {
                    promise.success(updateResponse);
                }
                @Override
                public void onFailure(Throwable throwable) {
                    if (throwable.getCause().getCause().getClass().equals(org.elasticsearch.index.engine.VersionConflictEngineException.class)) {
                        boolean done = false;
                        while(!done) {
                            done = true;
                            try {
                                T actual = get(id).get(10000);
                                try {
                                    UpdateResponse r = getClient().prepareUpdate(getIndexName(), getTypeName(), id)
                                            .setDoc(ESPlugin.getPlugin().getMapper().writeValueAsBytes(change.apply(actual)))
                                            .setVersion(actual.getVersion().get()).get();
                                } catch (JsonProcessingException e) {e.printStackTrace();}
                            } catch(VersionConflictEngineException e) {
                                if (ESPlugin.getPlugin().log())
                                    play.Logger.debug("Solved concurrency problem on " + getTypeName() + ", id="  + id);
                                done = false;
                            }
                        }
                    } else promise.failure(throwable.getCause());
                }
            });
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return F.Promise.wrap(promise.wrapped());
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
     * @param obj Object to parse
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

    private String parseToString(Object object) {
        try {
            return getMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } return null;
    }

    /**
     * Useful for more customized queries
     * @return The client from the official ES API.
     */
    public static Client getClient() {
        return ESPlugin.getPlugin().getClient();
    }

    /**
     * @return The jackson's Object Mapper that is used to parse the responses and for indexing
     */
    public static ObjectMapper getMapper() { return mapper; }

    private PutMappingResponse setMapping(XContentBuilder mapping) {
        try {
            return ESPlugin.getPlugin().getClient().admin().indices().preparePutMapping(getIndexName())
                    .setType(getTypeName())
                    .setSource(mapping)
                    .execute()
                    .get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }


}
