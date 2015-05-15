package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

public abstract class Index {

    private Optional<String> id;
    private Optional<Long> version;
    private long timestamp;
    
    public Index() {
        super();
        this.id = Optional.empty();
        this.version = Optional.empty();;
        this.timestamp = System.currentTimeMillis();
    }

    @JsonIgnore
    public Optional<String> getId() {
        return id;
    }

    public void setId(String id) {
        this.id = Optional.of(id);
    }

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

    public JsonNode toJson() {
        return ESPlugin.getPlugin().getMapper().convertValue(this, JsonNode.class);
    }

    public static <T extends Index> T fromJson(String data, Class<T> from) throws IOException {
        return ESPlugin.getPlugin().getMapper().readValue(data, from);
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Type {
        String name();
    }

}
