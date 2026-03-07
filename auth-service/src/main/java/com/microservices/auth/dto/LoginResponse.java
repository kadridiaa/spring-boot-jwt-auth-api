package com.microservices.auth.dto;

import java.time.LocalDateTime;

public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String email;
    private Long userId;
    private LocalDateTime timestamp;

    // Constructeurs
    public LoginResponse() {}

    public LoginResponse(boolean success, String message, String token, String email, Long userId, LocalDateTime timestamp) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.email = email;
        this.userId = userId;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
