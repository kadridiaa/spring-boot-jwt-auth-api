package com.softpaw.gatewayservice.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private ServerHttpResponse response;
    @Mock
    private GatewayFilterChain chain;

    private static final String SECRET_KEY = "mySecretKeyForJwtTokenGenerationThatShouldBeLongEnoughAndSecure12345";

    @BeforeEach
    void setUp() {
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getURI()).thenReturn(java.net.URI.create("/service-a/api/service-a/hello"));
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        when(response.setComplete()).thenReturn(Mono.empty());

        ReflectionTestUtils.setField(jwtAuthenticationFilter, "secretKey", SECRET_KEY);
    }

    private String generateTestToken(Long userId, String subject, List<String> permissions) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key)
                .compact();
    }

    @Test
    void shouldAddHeadersForValidJwt() {
        Long testUserId = 123L;
        String testUsername = "testuser";
        List<String> testPermissions = List.of("ACCESS_A", "ACCESS_B");
        String token = generateTestToken(testUserId, testUsername, testPermissions);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);

        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        jwtAuthenticationFilter.filter(exchange, chain).block();

        ArgumentCaptor<String> xUserCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> xUserIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> xPermissionsCaptor = ArgumentCaptor.forClass(String.class);

        verify(requestBuilder, times(1)).header(eq("X-User"), xUserCaptor.capture());
        verify(requestBuilder, times(1)).header(eq("X-User-Id"), xUserIdCaptor.capture());
        verify(requestBuilder, times(1)).header(eq("X-Permissions"), xPermissionsCaptor.capture());

        assertEquals(testUsername, xUserCaptor.getValue());
        assertEquals(String.valueOf(testUserId), xUserIdCaptor.getValue());
        assertEquals(String.join(",", testPermissions), xPermissionsCaptor.getValue());

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldReturnUnauthorizedForMissingToken() {
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        jwtAuthenticationFilter.filter(exchange, chain).block();

        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        when(request.getHeaders()).thenReturn(headers);

        jwtAuthenticationFilter.filter(exchange, chain).block();

        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldSkipFilterForPublicAuthPath() {
        when(request.getURI()).thenReturn(java.net.URI.create("/auth-service/api/auth/login"));
        jwtAuthenticationFilter.filter(exchange, chain).block();
        verify(chain, times(1)).filter(exchange);
        verify(request, never()).getHeaders();
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void shouldSkipFilterForOptionsRequests() {
        when(request.getMethod()).thenReturn(HttpMethod.OPTIONS);
        jwtAuthenticationFilter.filter(exchange, chain).block();
        verify(chain, times(1)).filter(exchange);
        verify(request, never()).getHeaders();
        verify(response, never()).setStatusCode(any());
    }
}
