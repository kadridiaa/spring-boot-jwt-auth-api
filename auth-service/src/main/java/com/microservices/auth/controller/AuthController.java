package com.microservices.auth.controller;

import com.microservices.auth.dto.*;
import com.microservices.auth.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return response.isSuccess() ? ResponseEntity.status(HttpStatus.CREATED).body(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<VerifyResponse> verify(@RequestParam String tokenId, @RequestParam String t) {
        VerifyResponse response = authService.verify(tokenId, t);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


  @PostMapping("/setup")
    public ResponseEntity<?> setupInitialAdmin(@Valid @RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.setupInitialAdmin(request));
        } catch (Exception e) {
            // REMPLACER ICI
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Collections.singletonMap("error", errorMsg));
        }
    }

    @PostMapping("/admin")
    public ResponseEntity<?> createAdmin(
            @RequestHeader(value = "X-User", required = false) String creatorFromGateway,
            @RequestHeader(value = "X-User-Email", required = false) String creatorFromLegacyHeader,
            @RequestParam Set<String> permissions,
            @Valid @RequestBody RegisterRequest request) {
        try {
            String creatorEmail = StringUtils.hasText(creatorFromGateway)
                    ? creatorFromGateway
                    : creatorFromLegacyHeader;

            if (!StringUtils.hasText(creatorEmail)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(java.util.Collections.singletonMap("error", "Missing creator identity header (X-User or X-User-Email)."));
            }

            return ResponseEntity.ok(authService.createAdmin(creatorEmail, request, permissions));
        } catch (RuntimeException e) {
            // REMPLACER ICI
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Access Denied";
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(java.util.Collections.singletonMap("error", errorMsg));
        }
    }

    @PutMapping("/permissions")
    public ResponseEntity<?> updatePermissions(
            @RequestHeader(value = "X-Permissions", defaultValue = "") String userPermissions,
            @RequestParam String email, 
            @RequestBody Set<String> permissions) {
            
        // Vérification que l'utilisateur qui fait la requête est un admin
        if (!userPermissions.contains("ALL_ACCESS")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("error", "Access Denied: Only Admin can modify permissions."));
        }

        try {
            authService.updatePermissions(email, permissions);
            return ResponseEntity.ok(Map.of("success", true, "message", "Permissions were updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "auth-service"));
    }
}