package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Index {

    private String id;
    private Long version;
    private long timestamp;
    
    public Index() {
        super();
        this.version = null;
        this.timestamp = System.currentTimeMillis();
    }

    public Optional<String> getId() {
        return Optional.of(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<Long> getVersion() {
        return Optional.of(version);
    }

    public void setVersion(Long version) {
        this.version = version;
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
    public @interface Entity {
        String type();
    }

}
