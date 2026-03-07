package com.microservices.auth.dto;

import java.time.LocalDateTime;

public class VerifyResponse {

    private boolean success;
    private String message;
    private String email;
    private LocalDateTime timestamp;

    // Constructeurs
    public VerifyResponse() {}

    public VerifyResponse(boolean success, String message, String email, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.email = email;
        this.timestamp = timestamp;
    }

    // Getters et Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
