package com.auth.controller;

import com.auth.dto.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur pour les endpoints publics (sans authentification)
 */
@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    /**
     * Endpoint de test public
     * GET /api/public/hello
     */
    @GetMapping("/hello")
    public ResponseEntity<?> hello() {
        return ResponseEntity.ok(
            new MessageResponse("Bonjour ! Ceci est un endpoint public accessible sans authentification.")
        );
    }

    /**
     * Endpoint pour vérifier que l'application fonctionne
     * GET /api/public/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(
            new MessageResponse("✅ L'application fonctionne correctement !")
        );
    }
}
