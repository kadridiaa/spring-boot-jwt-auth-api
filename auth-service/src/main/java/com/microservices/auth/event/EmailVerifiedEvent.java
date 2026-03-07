package com.microservices.auth.event;

import java.time.LocalDateTime;

public class EmailVerifiedEvent {

    private String eventId;
    private String userId;
    private LocalDateTime occurredAt;
    
    // Headers for tracing
    private String correlationId;
    private Integer schemaVersion = 1;

    // Constructeurs
    public EmailVerifiedEvent() {}

    public EmailVerifiedEvent(String eventId, String userId, LocalDateTime occurredAt, String correlationId, Integer schemaVersion) {
        this.eventId = eventId;
        this.userId = userId;
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
