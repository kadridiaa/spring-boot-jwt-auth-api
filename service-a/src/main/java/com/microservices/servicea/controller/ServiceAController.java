package com.microservices.servicea.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-a")
public class ServiceAController {

    @GetMapping("/hello")
    public ResponseEntity<String> sayHello(@RequestHeader(value = "X-Permissions", defaultValue = "") String permissions) {
        if (!permissions.contains("ACCESS_A")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Accès refusé. Vous n'avez pas la permission ACCESS_A.");
        }
        return ResponseEntity.ok("Hello! Je suis le Service A. Vous avez bien la permission de me voir !");
    }
}
