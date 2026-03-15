package com.auth.dto;

/**
 * DTO pour les messages d'erreur ou de succès
 */
public class MessageResponse {
    private String message;

    // Constructeurs
    public MessageResponse() {}

    public MessageResponse(String message) {
        this.message = message;
    }

    // Getters et Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
