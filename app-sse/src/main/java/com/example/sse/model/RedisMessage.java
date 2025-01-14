package com.example.sse.model;

import java.io.Serializable;

public class RedisMessage implements Serializable {
    private String type;
    private String clientId;
    private SseEvent event;

    public RedisMessage() {
    }

    public RedisMessage(String type, String clientId, SseEvent event) {
        this.type = type;
        this.clientId = clientId;
        this.event = event;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public SseEvent getEvent() {
        return event;
    }

    public void setEvent(SseEvent event) {
        this.event = event;
    }
} 