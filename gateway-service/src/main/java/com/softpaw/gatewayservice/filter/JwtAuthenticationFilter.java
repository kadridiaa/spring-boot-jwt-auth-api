package com.softpaw.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        System.out.println("Requête reçue pour : " + path);

        if (isPublicAuthPath(path)) {
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
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);

            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);
            String permsHeader = permissions != null ? String.join(",", permissions) : "";

            System.out.println("---------------------Permissions extraites : " + permsHeader);
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User", username)
                    .header("X-User-Email", username)
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Permissions", permsHeader)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            System.out.println("JWT invalide : " + e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicAuthPath(String path) {
        return path.endsWith("/api/auth/register")
                || path.endsWith("/api/auth/login")
                || path.endsWith("/api/auth/verify")
                || path.endsWith("/api/auth/setup")
                || path.endsWith("/api/auth/health");
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
