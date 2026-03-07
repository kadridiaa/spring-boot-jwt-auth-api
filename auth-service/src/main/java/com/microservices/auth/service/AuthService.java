package com.microservices.auth.service;

import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.dto.RegisterResponse;
import com.microservices.auth.dto.VerifyResponse;
import com.microservices.auth.entity.User;
import com.microservices.auth.entity.VerificationToken;
import com.microservices.auth.event.EmailVerifiedEvent;
import com.microservices.auth.event.UserRegisteredEvent;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EventPublisher eventPublisher;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${app.verification.token.ttl-minutes}")
    private int tokenTtlMinutes;

    // Constructeur pour injection de dépendances
    public AuthService(UserRepository userRepository, 
                      VerificationTokenRepository tokenRepository,
                      EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", maskEmail(request.getEmail()));

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Email already registered: {}", maskEmail(request.getEmail()));
            RegisterResponse response = new RegisterResponse();
            response.setSuccess(false);
            response.setMessage("Email already registered");
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Create user (verified=false)
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword() != null ? passwordEncoder.encode(request.getPassword()) : null);
        user.setVerified(false);

        user = userRepository.save(user);
        log.info("User created with ID: {}", user.getId());

        // Generate verification token
        String tokenId = UUID.randomUUID().toString();
        String tokenClear = UUID.randomUUID().toString(); // The actual token sent to user
        String tokenHash = passwordEncoder.encode(tokenClear); // Store only the hash

        VerificationToken token = new VerificationToken();
        token.setTokenId(tokenId);
        token.setUserId(user.getId());
        token.setTokenHash(tokenHash);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));

        tokenRepository.save(token);
        log.info("Verification token created: tokenId={}, expiresAt={}", tokenId, token.getExpiresAt());

        // Publish UserRegistered event
        String correlationId = UUID.randomUUID().toString();
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(String.valueOf(user.getId()));
        event.setEmail(request.getEmail());
        event.setTokenId(tokenId);
        event.setTokenClear(tokenClear); // Include clear token for notification service
        event.setOccurredAt(LocalDateTime.now());
        event.setCorrelationId(correlationId);
        event.setSchemaVersion(1);

        // TODO: Réactiver quand RabbitMQ sera disponible
        // eventPublisher.publishUserRegistered(event);

        RegisterResponse response = new RegisterResponse();
        response.setSuccess(true);
        response.setMessage("Registration successful. Please check your email to verify your account.");
        response.setUserId(user.getId());
        response.setEmail(maskEmail(user.getEmail()));
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    @Transactional
    public VerifyResponse verify(String tokenId, String tokenClear) {
        log.info("Verification attempt for tokenId: {}", tokenId);

        // Find token
        VerificationToken token = tokenRepository.findByTokenId(tokenId)
            .orElse(null);

        if (token == null) {
            log.warn("Token not found: {}", tokenId);
            VerifyResponse response = new VerifyResponse();
            response.setSuccess(false);
            response.setMessage("Invalid or expired verification link");
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Check expiration
        if (token.isExpired()) {
            log.warn("Token expired: tokenId={}, expiresAt={}", tokenId, token.getExpiresAt());
            tokenRepository.delete(token);
            VerifyResponse response = new VerifyResponse();
            response.setSuccess(false);
            response.setMessage("Verification link has expired. Please request a new one.");
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Verify token using BCrypt
        if (!passwordEncoder.matches(tokenClear, token.getTokenHash())) {
            log.warn("Token mismatch for tokenId: {}", tokenId);
            VerifyResponse response = new VerifyResponse();
            response.setSuccess(false);
            response.setMessage("Invalid verification link");
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Find user
        User user = userRepository.findById(token.getUserId())
            .orElse(null);

        if (user == null) {
            log.error("User not found for token: userId={}", token.getUserId());
            tokenRepository.delete(token);
            VerifyResponse response = new VerifyResponse();
            response.setSuccess(false);
            response.setMessage("User not found");
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Check if already verified (idempotence)
        if (user.isVerified()) {
            log.info("User already verified: userId={}", user.getId());
            tokenRepository.delete(token);
            VerifyResponse response = new VerifyResponse();
            response.setSuccess(true);
            response.setMessage("Email already verified");
            response.setEmail(maskEmail(user.getEmail()));
            response.setTimestamp(LocalDateTime.now());
            return response;
        }

        // Mark as verified
        user.setVerified(true);
        user.setVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Delete token (one-shot)
        tokenRepository.delete(token);

        log.info("User verified successfully: userId={}", user.getId());

        // Publish EmailVerified event (optional)
        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setUserId(String.valueOf(user.getId()));
        event.setOccurredAt(LocalDateTime.now());
        event.setCorrelationId(UUID.randomUUID().toString());
        event.setSchemaVersion(1);

        // TODO: Réactiver quand RabbitMQ sera disponible
        // eventPublisher.publishEmailVerified(event);

        VerifyResponse response = new VerifyResponse();
        response.setSuccess(true);
        response.setMessage("Email verified successfully!");
        response.setEmail(maskEmail(user.getEmail()));
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String maskedLocal = localPart.substring(0, Math.min(2, localPart.length())) + "***";
        return maskedLocal + "@" + parts[1];
    }
}
