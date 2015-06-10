package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.Optional;

/**
 * A object that is associated with a document on the ES server. Models to be saved in the cluster should
 * extend this class
 */
public abstract class Index {

    @Inject static ESComponent component;

    private Optional<String> id;
    private Optional<Long> version;
    private long timestamp;

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
        return component.getMapper().convertValue(this, JsonNode.class);
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
        return component.getMapper().readValue(data, from);
    }



}
