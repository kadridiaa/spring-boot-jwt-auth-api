package com.microservices.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.auth.dto.*;
import com.microservices.auth.model.AuthType;
import com.microservices.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("AuthController - Tests des endpoints REST")
class AuthControllerTest {

    private MockMvc mockMvc;

        private ObjectMapper objectMapper;

        private StubAuthService authService;

        @BeforeEach
        void setUp() {
                authService = new StubAuthService();
                objectMapper = new ObjectMapper();
                AuthController controller = new AuthController(authService);
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

    // =========================================================
    // POST /api/auth/register
    // =========================================================

    @Test
    @DisplayName("POST /register - inscription réussie → 201 Created")
    void register_success_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("alice@test.com", "secret");
        authService.registerResponse = new RegisterResponse(true, "Registration successful. Please check your email.",
                1L, "ali***@test.com", LocalDateTime.now());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Registration successful")));
    }

    @Test
    @DisplayName("POST /register - email déjà utilisé → 400 Bad Request")
    void register_emailAlreadyUsed_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("alice@test.com", "secret");
        authService.registerResponse = new RegisterResponse(false, "Email already registered",
                null, null, LocalDateTime.now());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Email already registered")));
    }

    // =========================================================
    // POST /api/auth/login
    // =========================================================

    @Test
    @DisplayName("POST /login - identifiants valides → 200 OK avec token JWT")
    void login_validCredentials_returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "secret", AuthType.PASSWORD);
        authService.loginResponse = new LoginResponse(true, "Login successful!",
                "eyJhbGciOiJIUzI1NiJ9.fakejwt", "alice@test.com", 1L, LocalDateTime.now());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("alice@test.com")));
    }

    @Test
    @DisplayName("POST /login - mauvaises credentials → 401 Unauthorized")
    void login_invalidCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "mauvais", AuthType.PASSWORD);
        authService.loginResponse = new LoginResponse(false, "Invalid credentials",
                null, null, null, LocalDateTime.now());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /login - email non vérifié → 401 Unauthorized")
    void login_emailNotVerified_returns401() throws Exception {
        LoginRequest request = new LoginRequest("alice@test.com", "secret", AuthType.PASSWORD);
        authService.loginResponse = new LoginResponse(false, "Email not verified. Please verify your email.",
                null, null, null, LocalDateTime.now());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("not verified")));
    }

    // =========================================================
    // GET /api/auth/verify
    // =========================================================

    @Test
    @DisplayName("GET /verify - token valide → 200 OK")
    void verify_validToken_returns200() throws Exception {
        authService.verifyResponse = new VerifyResponse(true, "Email verified successfully!",
                "ali***@test.com", LocalDateTime.now());

        mockMvc.perform(get("/api/auth/verify")
                        .param("tokenId", "tokenId-abc")
                        .param("t", "tokenClear-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Email verified successfully!")));
    }

    @Test
    @DisplayName("GET /verify - token invalide ou expiré → 400 Bad Request")
    void verify_invalidToken_returns400() throws Exception {
        authService.verifyResponse = new VerifyResponse(false, "Invalid or expired verification link",
                null, LocalDateTime.now());

        mockMvc.perform(get("/api/auth/verify")
                        .param("tokenId", "bad-token")
                        .param("t", "bad-clear"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // =========================================================
    // POST /api/auth/setup
    // =========================================================

    @Test
    @DisplayName("POST /setup - premier démarrage → 200 OK avec superadmin créé")
    void setup_firstTime_returns200() throws Exception {
        RegisterRequest request = new RegisterRequest("dido@example.com", "dido");
        authService.setupResponse = new RegisterResponse(true, "Admin created successfully",
                1L, "did***@example.com", LocalDateTime.now());

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("POST /setup - superadmin déjà existant → 400 Bad Request")
    void setup_alreadyExists_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("dido@example.com", "dido");
        authService.setupException = new RuntimeException("Admin already exists");

        mockMvc.perform(post("/api/auth/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("already exists")));
    }

    // =========================================================
    // PUT /api/auth/permissions
    // =========================================================

    @Test
    @DisplayName("PUT /permissions - admin avec ALL_ACCESS → 200 OK")
    void updatePermissions_withAllAccess_returns200() throws Exception {
        Set<String> perms = Set.of("ACCESS_A", "ACCESS_B");

        mockMvc.perform(put("/api/auth/permissions")
                        .param("email", "achour@test.com")
                        .header("X-Permissions", "ALL_ACCESS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perms)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    @DisplayName("PUT /permissions - sans ALL_ACCESS → 403 Forbidden")
    void updatePermissions_withoutAllAccess_returns403() throws Exception {
        Set<String> perms = Set.of("ACCESS_A");

        mockMvc.perform(put("/api/auth/permissions")
                        .param("email", "achour@test.com")
                        .header("X-Permissions", "ACCESS_A")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perms)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Access Denied")));
    }

    @Test
    @DisplayName("PUT /permissions - sans header X-Permissions → 403 Forbidden")
    void updatePermissions_noHeader_returns403() throws Exception {
        Set<String> perms = Set.of("ACCESS_A");

        mockMvc.perform(put("/api/auth/permissions")
                        .param("email", "achour@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(perms)))
                .andExpect(status().isForbidden());
    }

    // =========================================================
    // GET /api/auth/health
    // =========================================================

    @Test
    @DisplayName("GET /health → 200 OK avec status UP")
    void health_returns200() throws Exception {
        mockMvc.perform(get("/api/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("auth-service")));
    }

        private static class StubAuthService extends AuthService {
                RegisterResponse registerResponse = new RegisterResponse(true, "ok", 1L, "x@test.com", LocalDateTime.now());
                LoginResponse loginResponse = new LoginResponse(true, "ok", "token", "x@test.com", 1L, LocalDateTime.now());
                VerifyResponse verifyResponse = new VerifyResponse(true, "ok", "x@test.com", LocalDateTime.now());
                RegisterResponse setupResponse = new RegisterResponse(true, "ok", 1L, "x@test.com", LocalDateTime.now());
                RuntimeException setupException;

                StubAuthService() {
                        super(null, null, null, null, null, null);
                }

                @Override
                public RegisterResponse register(RegisterRequest request) {
                        return registerResponse;
                }

                @Override
                public LoginResponse login(LoginRequest request) {
                        return loginResponse;
                }

                @Override
                public VerifyResponse verify(String tokenId, String tokenClear) {
                        return verifyResponse;
                }

                @Override
                public RegisterResponse setupInitialAdmin(RegisterRequest request) {
                        if (setupException != null) {
                                throw setupException;
                        }
                        return setupResponse;
                }

                @Override
                public boolean updatePermissions(String email, Set<String> permissions) {
                        return true;
                }

                @Override
                public RegisterResponse createAdmin(String creatorEmail, RegisterRequest request, Set<String> newPermissions) {
                        return registerResponse;
                }
        }
}
