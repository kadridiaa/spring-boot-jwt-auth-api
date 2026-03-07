package com.microservices.notification.event;

import java.time.LocalDateTime;

public class UserRegisteredEvent {

    private String eventId;
    private String userId;
    private String email;
    private String tokenId;
    private String tokenClear;
    private LocalDateTime occurredAt;
    
    // Headers for tracing
    private String correlationId;
    private Integer schemaVersion = 1;

    // Constructeurs
    public UserRegisteredEvent() {}

    public UserRegisteredEvent(String eventId, String userId, String email, String tokenId, String tokenClear, LocalDateTime occurredAt, String correlationId, Integer schemaVersion) {
        this.eventId = eventId;
        this.userId = userId;
        this.email = email;
        this.tokenId = tokenId;
        this.tokenClear = tokenClear;
        this.occurredAt = occurredAt;
        this.correlationId = correlationId;
        this.schemaVersion = schemaVersion;
    }

    // Getters et Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenClear() {
        return tokenClear;
    }

    public void setTokenClear(String tokenClear) {
        this.tokenClear = tokenClear;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }
}
