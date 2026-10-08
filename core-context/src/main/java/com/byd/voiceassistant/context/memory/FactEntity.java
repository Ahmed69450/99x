package com.byd.voiceassistant.context.memory;

public class FactEntity {
    private long id;
    private String key;
    private String value;
    private String tags;
    private long timestamp;

    public FactEntity() {
    }

    public FactEntity(long id, String key, String value, String tags, long timestamp) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.tags = tags;
        this.timestamp = timestamp;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
