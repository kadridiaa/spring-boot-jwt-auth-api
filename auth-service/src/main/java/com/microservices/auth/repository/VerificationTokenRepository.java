package com.microservices.auth.repository;

import com.microservices.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    Optional<VerificationToken> findByTokenId(String tokenId);

    void deleteByUserId(Long userId);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}
