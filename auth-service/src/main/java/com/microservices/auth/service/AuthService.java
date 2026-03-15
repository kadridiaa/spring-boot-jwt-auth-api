package com.microservices.auth.service;

import com.microservices.auth.dto.*;
import com.microservices.auth.entity.User;
import com.microservices.auth.entity.VerificationToken;
import com.microservices.auth.event.EmailVerifiedEvent;
import com.microservices.auth.event.UserRegisteredEvent;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.repository.VerificationTokenRepository;
import com.microservices.auth.security.PermissionValidator;
import com.microservices.auth.strategy.AuthenticationStrategy;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EventPublisher eventPublisher;
    private final JwtService jwtService;
    private final List<AuthenticationStrategy> authStrategies;
    private final PermissionValidator permissionValidator;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.verification.token.ttl-minutes}")
    private int tokenTtlMinutes;

    public AuthService(UserRepository userRepository, 
                      VerificationTokenRepository tokenRepository,
                      EventPublisher eventPublisher,
                      JwtService jwtService,
                      List<AuthenticationStrategy> authStrategies,
                      PermissionValidator permissionValidator) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
        this.jwtService = jwtService;
        this.authStrategies = authStrategies;
        this.permissionValidator = permissionValidator;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", maskEmail(request.getEmail()));

        if (userRepository.existsByEmail(request.getEmail())) {
            return new RegisterResponse(false, "Email already registered", null, null, LocalDateTime.now());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null);
        user.setVerified(false);
        user = userRepository.save(user);

        String tokenId = UUID.randomUUID().toString();
        String tokenClear = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(tokenClear);

        VerificationToken token = new VerificationToken();
        token.setTokenId(tokenId);
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        tokenRepository.save(token);

        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(String.valueOf(user.getId()));
        event.setEmail(request.getEmail());
        event.setTokenId(tokenId);
        event.setTokenClear(tokenClear);
        event.setOccurredAt(LocalDateTime.now());
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setSchemaVersion(1);

        eventPublisher.publishUserRegistered(event);

        return new RegisterResponse(true, "Registration successful. Please check your email.", user.getId(), maskEmail(user.getEmail()), LocalDateTime.now());
    }

    @Transactional
    public VerifyResponse verify(String tokenId, String tokenClear) {
        VerificationToken token = tokenRepository.findByTokenId(tokenId).orElse(null);

        if (token == null || token.isExpired() || !passwordEncoder.matches(tokenClear, token.getTokenHash())) {
            if (token != null && token.isExpired()) tokenRepository.delete(token);
            return new VerifyResponse(false, "Invalid or expired verification link", null, LocalDateTime.now());
        }

        User user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null) {
            tokenRepository.delete(token);
            return new VerifyResponse(false, "User not found", null, LocalDateTime.now());
        }

        if (user.isVerified()) {
            tokenRepository.delete(token);
            return new VerifyResponse(true, "Email already verified", maskEmail(user.getEmail()), LocalDateTime.now());
        }

        user.setVerified(true);
        user.setVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        tokenRepository.delete(token);

        EmailVerifiedEvent event = new EmailVerifiedEvent(UUID.randomUUID().toString(), String.valueOf(user.getId()), LocalDateTime.now(), UUID.randomUUID().toString(), 1);
        eventPublisher.publishEmailVerified(event);

        return new VerifyResponse(true, "Email verified successfully!", maskEmail(user.getEmail()), LocalDateTime.now());
    }

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", maskEmail(request.getEmail()));
        try {
            // Utilisation du Design Pattern Strategy
            AuthenticationStrategy strategy = authStrategies.stream()
                .filter(s -> s.supports(request.getAuthType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported auth method"));

            User user = strategy.authenticate(request.getEmail(), request.getPassword());

            if (!user.isVerified()) {
                return new LoginResponse(false, "Email not verified. Please verify your email.", null, null, null, LocalDateTime.now());
            }

            // Génération du JWT avec les permissions incluses
            String token = jwtService.generateToken(user.getEmail(), user.getId(), user.getPermissions());
            return new LoginResponse(true, "Login successful!", token, user.getEmail(), user.getId(), LocalDateTime.now());

        } catch (Exception e) {
            log.warn("Login failed: {}", e.getMessage());
            return new LoginResponse(false, "Invalid credentials", null, null, null, LocalDateTime.now());
        }
    }

    @Transactional
    public RegisterResponse setupInitialAdmin(RegisterRequest request) {
        boolean superAdminExists = userRepository.findAll().stream()
            .map(User::getPermissions)
            .anyMatch(permissions -> permissions != null && permissions.contains("ALL_ACCESS"));

        if (superAdminExists) {
            throw new RuntimeException("Initial setup already done! Cannot create super admin.");
        }
        RegisterResponse response = this.register(request);
        User admin = userRepository.findByEmail(request.getEmail()).orElseThrow();
        admin.setPermissions(new HashSet<>(Set.of("ALL_ACCESS")));
        userRepository.save(admin);
        return response;
    }

    @Transactional
    public boolean updatePermissions(String email, Set<String> permissions) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPermissions(permissions);
        userRepository.save(user);
        return true;
    }

    public RegisterResponse createAdmin(String creatorEmail, RegisterRequest request, Set<String> newPermissions) {
        User creator = userRepository.findByEmail(creatorEmail)
            .orElseThrow(() -> new RuntimeException("Creator not found"));

        if (!permissionValidator.hasPermission(creator.getPermissions(), "CREATE_ADMIN")) {
            throw new RuntimeException("Access Denied: You lack the CREATE_ADMIN permission");
        }

        RegisterResponse response = this.register(request);
        User newAdmin = userRepository.findByEmail(request.getEmail()).orElseThrow();
        newAdmin.setPermissions(newPermissions);
        userRepository.save(newAdmin);
        return response;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(2, parts[0].length())) + "***@" + parts[1];
    }
}