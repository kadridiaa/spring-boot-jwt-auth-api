package com.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Classe principale de l'application Spring Boot
 * @SpringBootApplication active :
 * - @Configuration : permet la configuration des beans
 * - @EnableAutoConfiguration : configuration automatique de Spring
 * - @ComponentScan : scan des composants dans le package
 */
@SpringBootApplication
public class SpringAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAuthApplication.class, args);
        System.out.println("\n✅ Application démarrée sur http://localhost:8080");
        System.out.println("📊 Console H2 : http://localhost:8080/h2-console\n");
    }
}
