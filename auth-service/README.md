# Authentication Service

Microservice d'authentification utilisant le **Strategy Design Pattern** pour gérer plusieurs méthodes d'authentification.

## 🎯 Fonctionnalités

- **Authentification Multi-Stratégies** :
  - Authentification locale (username/email + password)
  - Google OAuth
  - GitHub OAuth
  - Extensible pour d'autres providers (Facebook, etc.)

- **Base de données H2** en mémoire
- **API RESTful** bien structurée
- **DTOs** pour la validation et le transfert de données
- **Architecture microservices** ready

## 🏗️ Architecture

### Strategy Design Pattern

Le service utilise le pattern Strategy pour permettre différentes méthodes d'authentification sans modifier le code principal :

```
AuthenticationService (Context)
    ↓
AuthenticationStrategy (Interface)
    ↓
├── LocalAuthenticationStrategy
├── GoogleAuthenticationStrategy
└── GitHubAuthenticationStrategy
```

### Structure du Projet

```
auth-service/
├── src/main/java/com/microservices/auth/
│   ├── AuthServiceApplication.java          # Point d'entrée
│   ├── controller/
│   │   └── AuthController.java              # REST endpoints
│   ├── service/
│   │   └── AuthenticationService.java       # Service principal (Context)
│   ├── strategy/
│   │   ├── AuthenticationStrategy.java      # Interface Strategy
│   │   ├── LocalAuthenticationStrategy.java # Stratégie locale
│   │   ├── GoogleAuthenticationStrategy.java# Stratégie Google
│   │   └── GitHubAuthenticationStrategy.java# Stratégie GitHub
│   ├── dto/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── OAuthLoginRequest.java
│   │   └── AuthResponse.java
│   ├── entity/
│   │   └── User.java                        # Entité JPA
│   └── repository/
│       └── UserRepository.java              # Repository JPA
└── src/main/resources/
    └── application.properties               # Configuration
```

## 🚀 Démarrage

### Prérequis
- Java 17+
- Maven 3.6+

### Lancer localement

```bash
# Depuis le dossier auth-service
mvn spring-boot:run
```

### Lancer avec Docker

```bash
# Build
docker build -t auth-service:1.0.0 .

# Run
docker run -p 8080:8080 auth-service:1.0.0
```

## 📡 API Endpoints

### 1. Inscription (Local)
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "provider": "LOCAL"
}
```

### 2. Connexion (Local)
```http
POST /api/auth/login
Content-Type: application/json

{
  "identifier": "john_doe",  // ou email
  "password": "password123",
  "provider": "LOCAL"
}
```

### 3. Connexion OAuth (Google/GitHub)
```http
POST /api/auth/oauth/login
Content-Type: application/json

{
  "accessToken": "google_or_github_token",
  "provider": "GOOGLE",
  "email": "user@gmail.com",
  "username": "user",
  "providerId": "123456789"
}
```

### 4. Liste des providers supportés
```http
GET /api/auth/providers
```

### 5. Health Check
```http
GET /api/auth/health
```

## 🗄️ Base de Données

### H2 Console
Accessible à : `http://localhost:8080/h2-console`

**Paramètres de connexion :**
- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: _(vide)_

### Schéma User
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);
```

## 🎨 Bonnes Pratiques Implémentées

1. **Strategy Pattern** : Séparation des algorithmes d'authentification
2. **DTOs** : Validation et transfert de données propres
3. **Lombok** : Réduction du boilerplate code
4. **Logging** : Logs structurés avec SLF4J
5. **Validation** : Validation des inputs avec Jakarta Validation
6. **RESTful API** : Conventions REST respectées
7. **Docker** : Multi-stage build optimisé
8. **Health Checks** : Monitoring de la santé du service
9. **Architecture Microservices** : Service indépendant et déployable

## 🔧 Configuration

Variables d'environnement disponibles :

```bash
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:h2:mem:authdb
SPRING_JPA_HIBERNATE_DDL_AUTO=create-drop
```

## 📝 TODOs pour Production

- [ ] Implémenter BCrypt pour le hashing des mots de passe
- [ ] Ajouter JWT pour les tokens d'authentification
- [ ] Intégrer les vraies APIs OAuth (Google, GitHub)
- [ ] Ajouter rate limiting
- [ ] Ajouter refresh tokens
- [ ] Implémenter password reset
- [ ] Ajouter email verification
- [ ] Migrer vers une BDD persistante (PostgreSQL)
- [ ] Ajouter des tests unitaires et d'intégration
- [ ] Implémenter CORS configuration plus stricte

## 🧪 Tester l'API

### Avec cURL

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com","password":"test123","provider":"LOCAL"}'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"identifier":"testuser","password":"test123","provider":"LOCAL"}'
```

## 📚 Documentation Supplémentaire

- [Strategy Design Pattern](https://refactoring.guru/design-patterns/strategy)
- [Spring Boot Best Practices](https://spring.io/guides)
- [Microservices Architecture](https://microservices.io/)
