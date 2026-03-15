package com.microservices.auth.dto;

import com.microservices.auth.model.AuthType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private AuthType authType = AuthType.PASSWORD; // Par défaut

    // Constructeurs
    public LoginRequest() {}

    public LoginRequest(String email, String password, AuthType authType) {
        this.email = email;
        this.password = password;
        this.authType = authType;
    }

    // Getters et Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public AuthType getAuthType() { return authType; }
    public void setAuthType(AuthType authType) { this.authType = authType; }
}