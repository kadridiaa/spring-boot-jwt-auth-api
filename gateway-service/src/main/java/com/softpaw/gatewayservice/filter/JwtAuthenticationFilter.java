package com.softpaw.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final String SECRET_KEY = "SuperSecretKeyThatIsAtLeast32CharsLong!!!"; // Doit correspondre à celle de auth-service

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            System.out.println("Requête reçue pour : " + path);

            // Skip JWT pour certains endpoints publics seulement
            if (path.equals("/api/auth/register") || 
                path.equals("/api/auth/login") || 
                path.equals("/api/auth/verify") || 
                path.equals("/api/auth/setup") ||
                path.equals("/api/auth/health")) {
                return chain.filter(exchange);
            }

            String token = request.getHeaders().getFirst("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                System.out.println("Token absent ou incorrect");
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            token = token.substring(7);

            try {
                SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                Long userId = claims.get("userId", Long.class);

                // --- EXTRACTION ET PROPAGATION DES PERMISSIONS ---
                @SuppressWarnings("unchecked")
                List<String> permissions = claims.get("permissions", List.class);
                String permsHeader = permissions != null ? String.join(",", permissions) : "";
                
                System.out.println("---------------------Permissions extraites : "+ permsHeader);
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User", username)
                        .header("X-User-Id", String.valueOf(userId))
                        .header("X-Permissions", permsHeader) // 👈 Ajout du header des permissions
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                System.out.println("JWT invalide : " + e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }


    public static class Config {
        // Configuration du filtre (optionnel)
    }
}
