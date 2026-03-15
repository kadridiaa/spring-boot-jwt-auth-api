package com.auth.repository;

import com.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository pour l'entité User
 * JpaRepository fournit les méthodes CRUD de base
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Trouve un utilisateur par son nom d'utilisateur
     * @param username le nom d'utilisateur
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Trouve un utilisateur par son email
     * @param email l'email de l'utilisateur
     * @return Optional contenant l'utilisateur si trouvé
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Vérifie si un utilisateur existe avec ce nom d'utilisateur
     * @param username le nom d'utilisateur
     * @return true si existe, false sinon
     */
    Boolean existsByUsername(String username);
    
    /**
     * Vérifie si un utilisateur existe avec cet email
     * @param email l'email
     * @return true si existe, false sinon
     */
    Boolean existsByEmail(String email);
}
