package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import org.elasticsearch.common.xcontent.XContentBuilder;
import play.libs.Json;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * A object that is associated with a document on the ES server. Models to be saved in the cluster should
 * extend this class
 */
public abstract class Index {

    private Optional<String> id;
    private Optional<Long> version;
    private long timestamp;

    @JsonIgnore
    private static Finder finder = null;

    public static <T extends Index> void registerAsType(Class<T> model, ES es) {
        finder = new Finder<T>(model, es.getClient(), es.indexName());
    }

    public static <T extends Index> Finder<T> finder(Class<T> from) {
        return (Finder<T>) finder;
    }

    public static <T extends Index> Finder<T> finder(Class<T> from, Consumer<XContentBuilder> consumer) {
        XContentBuilder builder = null;
        try {
            builder = jsonBuilder().startObject();
            builder.startObject(finder.getType());
            consumer.accept(builder);
            builder.endObject();
            builder.endObject();
            finder.setMapping(builder);
        } catch (IOException e) {e.printStackTrace();}
        return (Finder<T>) finder;
    }

    public Index() {
        super();
        this.id = Optional.empty();
        this.version = Optional.empty();;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     *
     * @return The id of the document that inherent to the document on the ES server
     * This value only exists if the Index object was queried from the db or indexed before,
     * otherwise this Option will be empty.
     */
    @JsonIgnore
    public Optional<String> getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Optional.of(id);
    }

    /**
     *
     * @return The version of the index that is inherent to the document on the ES server
     * This value only exists if the Index object was queried from the db or indexed before,
     * otherwise this Option will be empty.
     */
    @JsonIgnore
    public Optional<Long> getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = Optional.of(version);
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Uses the default ESPlugin Object Mapper to create a Json object
     * @return A json object that is compatible with Play's controllers.
     */
    public JsonNode toJson() {
        return Json.mapper().convertValue(this, JsonNode.class);
    }

    /**
     * Parses Json to an index
     * @param data A Json string
     * @param from The class of the object that the Json string will be parsed to
     * @param <T> A type that extends an Index, meaning your Play Model
     * @return A java object parsed from the Json content.
     * @throws IOException
     */
    public static <T extends Index> T fromJson(String data, Class<T> from) throws IOException {
        return Json.mapper().readValue(data, from);
    }



}
