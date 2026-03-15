package com.microservices.servicea.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceAController.class)
@DisplayName("ServiceAController - Tests d'accès avec permissions ACCESS_A")
class ServiceAControllerTest {

    private static final String SECRET = "mySecretKeyForJwtTokenGenerationThatShouldBeLongEnoughAndSecure12345";

    @Autowired
    private MockMvc mockMvc;

    // ─── helpers ────────────────────────────────────────────────────────────────

    /** Génère un JWT signé contenant la liste de permissions fournie. */
    private String buildJwt(List<String> permissions) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", 1L);
        claims.put("email", "test@test.com");
        claims.put("permissions", permissions);

        return Jwts.builder()
                .claims(claims)
                .subject("test@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    // ─── via header X-Permissions (injecté par le gateway) ──────────────────────

    @Test
    @DisplayName("GET /hello - header X-Permissions: ACCESS_A → 200 OK")
    void hello_withXPermissionsAccessA_returns200() throws Exception {
        mockMvc.perform(get("/api/service-a/hello")
                        .header("X-Permissions", "ACCESS_A"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Service A")));
    }

    @Test
    @DisplayName("GET /hello - header X-Permissions: ACCESS_B seulement → 403 Forbidden")
    void hello_withXPermissionsAccessBOnly_returns403() throws Exception {
        mockMvc.perform(get("/api/service-a/hello")
                        .header("X-Permissions", "ACCESS_B"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("ACCESS_A")));
    }

    @Test
    @DisplayName("GET /hello - header X-Permissions avec ACCESS_A et ACCESS_B → 200 OK")
    void hello_withXPermissionsMultiple_returns200() throws Exception {
        mockMvc.perform(get("/api/service-a/hello")
                        .header("X-Permissions", "ACCESS_A,ACCESS_B"))
                .andExpect(status().isOk());
    }

    // ─── via JWT directement (fallback si gateway absent) ───────────────────────

    @Test
    @DisplayName("GET /hello - JWT avec ACCESS_A (pas de X-Permissions) → 200 OK")
    void hello_withJwtContainingAccessA_returns200() throws Exception {
        String jwt = buildJwt(List.of("ACCESS_A", "ACCESS_B"));

        mockMvc.perform(get("/api/service-a/hello")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Service A")));
    }

    @Test
    @DisplayName("GET /hello - JWT sans ACCESS_A → 403 Forbidden")
    void hello_withJwtMissingAccessA_returns403() throws Exception {
        String jwt = buildJwt(List.of("ACCESS_B"));

        mockMvc.perform(get("/api/service-a/hello")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /hello - JWT invalide (mauvaise signature) → 403 Forbidden")
    void hello_withInvalidJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/service-a/hello")
                        .header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isForbidden());
    }

    // ─── sans aucune authentification ────────────────────────────────────────────

    @Test
    @DisplayName("GET /hello - sans token ni header → 403 Forbidden")
    void hello_withoutAnyAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/service-a/hello"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /hello - header Authorization sans 'Bearer ' → 403 Forbidden")
    void hello_withMalformedAuthHeader_returns403() throws Exception {
        mockMvc.perform(get("/api/service-a/hello")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isForbidden());
    }
}
