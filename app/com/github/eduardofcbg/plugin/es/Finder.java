package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
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
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import play.libs.F;
import play.libs.F.Promise;
import play.libs.F.RedeemablePromise;
import play.libs.Json;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;


/**
 * Helper class that queries your elasticsearch cluster. Should be instantiated statically in your models, meaning
 * that is is associated with each ES type.
 * @param <T> The type of your model
 */
public class Finder<T extends Index> {

    private Class<T> from;

    private static Client esClient = null;
    private static String EsIndexName = null;

    /**
     * Creates a finder for querying ES cluster
     */
    public Finder(Class<T> from, Client client, String indexName) {
        this.from = from;
        esClient = client;
        EsIndexName = indexName;
        try {
            setParentMapping();
            setNestedFields();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Promise<IndexResponse> index(T toIndex, Consumer<IndexRequestBuilder> consumer) {
        return indexChild(toIndex, null, consumer);
    }

    /**
     * Indexes a model as a document in the cluster.
     * The model object indexed will then have associated with it a version and an id.
     * @param toIndex The model to be indexed
     * @return A promise (async) of the response given by the server
     */
    public Promise<IndexResponse> index(T toIndex) {
        return indexChild(toIndex, null, null);
    }

    /**
     * Indexes a document as a child of another document
     * @param toIndex
     * @param parentId
     * @return
     */
    public Promise<IndexResponse> indexChild(T toIndex, String parentId) {
        return indexChild(toIndex, parentId, null);
    }

    /**
     * Indexes a model which is a children of specified document id.
     * The model object indexed will then have associated with it a version and an id.
     * @param toIndex The model to be indexed
     * @return A promise (async) of the response given by the server
     */
    public Promise<IndexResponse> indexChild(T toIndex, String parentId, Consumer<IndexRequestBuilder> consumer) {
        IndexRequestBuilder builder = null;
        try {
            builder = getClient().prepareIndex(getIndex(), getType())
                    .setSource(Json.mapper().writeValueAsBytes(toIndex));
        } catch (JsonProcessingException e) { e.printStackTrace();}
        if (parentId != null) builder.setParent(parentId);
        if (consumer != null) consumer.accept(builder);

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

    public Promise<BulkResponse> indexBulk(List<T> toIndex, Consumer<BulkItemResponse> consumer) {
        BulkRequestBuilder bulkRequest = getClient().prepareBulk();
        toIndex.forEach(data -> {
            try {
                bulkRequest.add(getClient().prepareIndex(getIndex(), getType()).setSource(Json.mapper().writeValueAsBytes(data)));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });

        RedeemablePromise<BulkResponse> promise = RedeemablePromise.empty();
        bulkRequest.execute().addListener(new ActionListener<BulkResponse>() {
            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                if (bulkItemResponses.hasFailures()) {
                    play.Logger.debug("ES - Bulk index failed");
                    bulkItemResponses.forEach(r -> consumer.accept(r));
                } else play.Logger.debug("ES - Bulk finished with errors");
                promise.success(bulkItemResponses);

            }

            @Override
            public void onFailure(Throwable throwable) {
                promise.failure(throwable);
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }

    /**
     * Gets a document from the elasticsearch cluster
     * @param id The unique id of the document that exists in the cluster
     * @return A promise (async) of the response given by the server
     * @throws java.lang.NullPointerException When the document is not found
     */
    public Promise<T> get(String id) {
        return getChild(id, null);
    }

    /**
     * Gets a child document from the elasticsearch cluster
     * @param id The unique id of the document that exists in the cluster
     * @return A promise (async) of the response given by the server
     * @throws java.lang.NullPointerException When the document is not found
     */
    public Promise<T> getChild(String id, String parentId) {
        GetRequestBuilder builder = getClient().prepareGet(getIndex(), getType(), id);
        if (parentId != null) builder.setParent(parentId);

        RedeemablePromise<T> promise = RedeemablePromise.empty();
        builder.execute(new ActionListener<GetResponse>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                promise.success(parse(getResponse));
            }
            @Override
            public void onFailure(Throwable throwable) {
                throwable.printStackTrace();
                promise.failure(throwable.getCause().getCause());
            }
        });
        return F.Promise.wrap(promise.wrapped());
    }

    /**
     * Deletes a document from elasticsearch
     * @param id
     * @return
     */
    public Promise<DeleteResponse> delete(String id) {
        return deleteChild(id, null);
    }

    /**
     * Deletes a child document from elasticsearch
     * @param id Id of the document associated with the model object you want to delete
     * @param parentId Id of tha parent document associated with the one deleting
     * @return A promise (async) of the response given by the server
     * @throws java.lang.NullPointerException When a document to delete is not found
     */
    public Promise<DeleteResponse> deleteChild(String id, String parentId) {
        DeleteRequestBuilder builder = getClient().prepareDelete(getIndex(), getType(), id);
        if (parentId != null) builder.setParent(parentId);

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
        CountRequestBuilder builder = getClient().prepareCount(getIndex());
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
    public Promise<List<T>> search(Consumer<SearchRequestBuilder> consumer, int page) {
        SearchRequestBuilder builder = getClient().prepareSearch(getIndex()).setTypes(getType());
        builder.setFrom(page*resultsPerPage()).setSize(resultsPerPage());
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
        return update(original, original.getId().get(), null, change);
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
        return update(original, original.getId().get(), null, change);
    }

    public Promise<UpdateResponse> updateChild(String id, String parentId, Function<T, T> change) {
        T original = get(id).get(10000);
        return update(original, original.getId().get(), parentId, change);
    }

    public Promise<UpdateResponse> updateChild(T original, String parentId, Function<T, T> change) {
        return update(original, original.getId().get(), parentId, change);
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
        return update(original, original.getId().get(), null, change);
    }

    /**
     * Update a document with a specified id and a specified original state for the object
     * @param original The original java object which state will be changed
     * @param change The function that can be passed that takes the original document and returns the one to be saved
     * @return The function that can be passed that takes the original document and returns the one to be saved
     * @throws java.lang.NullPointerException When a document to update is not found
     */
    public Promise<UpdateResponse> update(T original, String id, String parentId, Function<T, T> change) {
        T toUpdate = change.apply(original);

        UpdateRequestBuilder builder;
        RedeemablePromise<UpdateResponse> promise = RedeemablePromise.empty();
        try {
            builder = getClient().prepareUpdate(getIndex(), getType(), id)
                    .setDoc(Json.mapper().writeValueAsBytes(toUpdate))
                    .setVersion(original.getVersion().get());

            if (parentId != null) builder.setParent(parentId);
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
                                    UpdateResponse r = getClient().prepareUpdate(getIndex(), getType(), id)
                                            .setDoc(Json.mapper().writeValueAsBytes(change.apply(actual)))
                                            .setVersion(actual.getVersion().get()).get();
                                } catch (JsonProcessingException e) {e.printStackTrace();}
                            } catch(VersionConflictEngineException e) {
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
     * The name of the type of the model's finder.
     * @return Simply the annotated type name on the model
     */
    public String getType() {
        return getType(from);
    }

    public static <B extends Index> String getType(Class<B> type) {
        return type.getAnnotation(Type.Name.class).value();
    }

    /**
     * Additional method for parsing objects
     * @param obj Object to parse
     * @return
     */
    public static Map<String, Object> parse(Object obj) {
        return Json.mapper().convertValue(obj, Map.class);
    }

    public List<T> parse(SearchResponse hits) {
        return parse(hits, from);
    }

    /**
     * Additional method for parsing objects
     * @param hits The response from the cluster.
     * @return The list of models that were queried from the ES server.
     */
    public static <B extends Index> List<B> parse(SearchResponse hits, Class<B> from) {
        List<B> beans = new ArrayList<>();
        SearchHit[] newHits = hits.getHits().getHits();
        for(int i = 0; i < newHits.length; i++) {
            final SearchHit hit = newHits[i];
            B bean = Json.mapper().convertValue(hit.getSource(), from);
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
        T bean = Json.mapper().convertValue(result.getSource(), from);
        bean.setId(result.getId());
        bean.setVersion(result.getVersion());
        return bean;
    }

    public <B extends Index> F.Promise<List<T>> getAsChildrenOf(Class<B> parentType, String parentId, QueryBuilder query, int page) {
        return getAsChildrenOf(parentType, parentId, query, null, page);
    }

    public <B extends Index> F.Promise<List<T>> getAsChildrenOf(Class<B> parentType, QueryBuilder query, int page) {
        return getAsChildrenOf(parentType, query, null, page);
    }

    public <B extends Index> F.Promise<List<T>> getAsChildrenOf(Class<B> parentType, String parentId, QueryBuilder query, Consumer<SearchRequestBuilder> consumer, int page) {
        return search(s -> {
            s.setQuery(QueryBuilders.hasParentQuery(getType(parentType), QueryBuilders.filteredQuery(query,
                    FilterBuilders.termFilter("_id", parentId))));
            if (consumer != null)
                consumer.accept(s);
        }, page);
    }

    public <B extends Index> F.Promise<List<T>> getAsChildrenOf(Class<B> parentType, QueryBuilder query, Consumer<SearchRequestBuilder> consumer, int page) {
        return search(s -> {
            s.setQuery(QueryBuilders.hasParentQuery(parentType.getTypeName(), query));
            if (consumer != null)
                consumer.accept(s);
        }, page);
    }

    /**
     * All the children of this type. The children are the documents annotated as @Parent(value="thistype")
     * and that were indexed by passing the parent's id
     * @param parentType Type name of the parent document
     * @return Promise (async) of a list of parsed search results
     */
    public <B extends Index> F.Promise<List<T>> getAllAsChildrenOf(Class<B> parentType, int page) {
        return search(s -> s.setQuery(QueryBuilders.hasParentQuery(parentType.getTypeName(), matchAllQuery())), page);
    }

    /**
     * All the children of a specified object of this type.
     * and that were indexed by passing the parent's id
     * @param parentType Type name of the parent document
     * @return Promise (async) of a list of parsed search results
     */
    public <B extends Index> F.Promise<List<T>> getAsChildrenOf(Class<B> parentType, String parentId, int page) {
        return search(s -> {
            s.setQuery(QueryBuilders.hasParentQuery(getType(parentType), QueryBuilders.filteredQuery(QueryBuilders.matchAllQuery(),
                    FilterBuilders.termFilter("_id", parentId))));
        }, page);
    }


    public PutMappingResponse setMapping(XContentBuilder mapping) {
        try {
            return getClient().admin().indices().preparePutMapping(getIndex())
                    .setType(getType())
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

    private void setParentMapping() throws IOException {
        if (getParentType() != null) {
            XContentBuilder builder = jsonBuilder()
                    .startObject()
                    .startObject(getType())
                    .startObject("_parent")
                    .field("type", getParentType())
                    .endObject()
                    .endObject()
                    .endObject();
            setMapping(builder);
            play.Logger.debug("Set " + getParentType() + " as parent of " + getType());
        }
    }


    private void setNestedFields() throws IOException {
        for(Field field : from.getDeclaredFields()){
            if (field.isAnnotationPresent(Type.NestedField.class))
                setAsNested(field);
        }
    }

    private void setAsNested(Field field) throws IOException {
        XContentBuilder builder = jsonBuilder()
                .startObject()
                    .startObject(getType())
                        .startObject("properties")
                            .startObject(field.getName())
                                .field("type", "nested")
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
        setMapping(builder);
        play.Logger.debug("ES - Set " + field.getName() + " from " + getType() + " as nested field");
    }

    public String getParentType() {
        try {
            return from.getAnnotation(Type.Parent.class).value();
        } catch(NullPointerException e) {} return null;
    }

    public int resultsPerPage() {
        try {
            return from.getAnnotation(Type.ResultsPerPage.class).value();
        } catch(NullPointerException e) {} return 5;
    }

    public static String getIndex() {
        return EsIndexName;
    }

    public static Client getClient() {
        return esClient;
    }

}
