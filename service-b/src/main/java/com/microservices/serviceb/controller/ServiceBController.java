package com.microservices.serviceb.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/service-b")
public class ServiceBController {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello(
            @RequestHeader(value = "X-Permissions", defaultValue = "") String permissions,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        permissions = resolvePermissions(permissions, authorizationHeader);

        if (!permissions.contains("ACCESS_B")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé. Vous n'avez pas la permission ACCESS_B.");
        }
        return ResponseEntity.ok("Hello! Je suis le Service B. Vous avez bien la permission de me voir !");
    }

    private String resolvePermissions(String permissions, String authorizationHeader) {
        if (permissions != null && !permissions.isBlank()) {
            return permissions;
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return "";
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authorizationHeader.substring(7))
                    .getPayload();

            @SuppressWarnings("unchecked")
            List<String> jwtPermissions = claims.get("permissions", List.class);
            return jwtPermissions != null ? String.join(",", jwtPermissions) : "";
        } catch (Exception e) {
            return "";
        }
    }
}
