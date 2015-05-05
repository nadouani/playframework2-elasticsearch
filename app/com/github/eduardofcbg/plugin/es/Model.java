package com.github.eduardofcbg.plugin.es;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class Model {

    private String id;
    private Long version;
    private long timestamp;

    public Model() {
        super();
        this.version = null;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
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

    public static <T extends Model> T fromJson(String data, Class<T> from) throws IOException {
        return ESPlugin.getPlugin().getMapper().readValue(data, from);
    }

}
