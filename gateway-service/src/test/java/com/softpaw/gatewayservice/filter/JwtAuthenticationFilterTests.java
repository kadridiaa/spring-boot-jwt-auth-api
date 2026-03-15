package com.softpaw.gatewayservice.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private final String SECRET_KEY = "SuperSecretKeyThatIsAtLeast32CharsLong!!!";

    @BeforeEach
    void setUp() {
        // Mock initial request and response
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getURI()).thenReturn(java.net.URI.create("/some-protected-path"));
    }

    private String generateTestToken(Long userId, String subject, List<String> roles) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("userId", userId)
                .claim("roles", roles)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY)
                .compact();
    }

    @Test
    void shouldAddHeadersForValidJwt() {
        // Given
        Long testUserId = 123L;
        String testUsername = "testuser";
        List<String> testRoles = Arrays.asList("USER", "ADMIN");
        String token = generateTestToken(testUserId, testUsername, testRoles);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        when(request.getHeaders()).thenReturn(headers);

        // Mock the mutate() call on the request
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        // Mock the exchange.mutate() call
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        filter.filter(exchange, chain).block();

        // Then
        ArgumentCaptor<String> xUserCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> xUserIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> xRolesCaptor = ArgumentCaptor.forClass(String.class);

        verify(requestBuilder, times(1)).header(eq("X-User"), xUserCaptor.capture());
        verify(requestBuilder, times(1)).header(eq("X-User-Id"), xUserIdCaptor.capture());
        verify(requestBuilder, times(1)).header(eq("X-Roles"), xRolesCaptor.capture());

        assertEquals(testUsername, xUserCaptor.getValue());
        assertEquals(String.valueOf(testUserId), xUserIdCaptor.getValue());
        assertEquals(String.join(",", testRoles), xRolesCaptor.getValue());

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }

    @Test
    void shouldReturnUnauthorizedForMissingToken() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(response.setComplete()).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        filter.filter(exchange, chain).block();

        // Then
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldReturnUnauthorizedForInvalidToken() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        when(request.getHeaders()).thenReturn(headers);
        when(response.setComplete()).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        filter.filter(exchange, chain).block();

        // Then
        verify(response, times(1)).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(response, times(1)).setComplete();
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldSkipFilterForAuthPath() {
        // Given
        when(request.getURI()).thenReturn(java.net.URI.create("/auth/login"));
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        GatewayFilter filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        filter.filter(exchange, chain).block();

        // Then
        verify(chain, times(1)).filter(exchange);
        verify(request, never()).getHeaders(); // Ensure headers are not even checked
        verify(response, never()).setStatusCode(any());
    }
}
