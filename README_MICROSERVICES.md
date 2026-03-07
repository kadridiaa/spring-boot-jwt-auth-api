# 🏗️ Architecture Microservices - Authentification

Projet d'architecture microservices modulaire avec service d'authentification utilisant le **Strategy Design Pattern**.

## 📋 Table des Matières

- [Vue d'ensemble](#vue-densemble)
- [Architecture](#architecture)
- [Services](#services)
- [Technologies](#technologies)
- [Démarrage Rapide](#démarrage-rapide)
- [Documentation](#documentation)

## 🎯 Vue d'ensemble

Ce projet implémente une architecture microservices extensible, actuellement composée d'un service d'authentification robuste. L'architecture est conçue pour faciliter l'ajout de nouveaux services (mail, notification, nginx, etc.).

### Caractéristiques Clés

✅ **Architecture Microservices** avec Maven multi-modules  
✅ **Strategy Design Pattern** pour l'authentification multi-providers  
✅ **Base de données H2** pour le développement rapide  
✅ **Docker & Docker Compose** pour le déploiement  
✅ **API RESTful** bien structurée  
✅ **Bonnes pratiques** Spring Boot respectées  
✅ **Health checks** et monitoring  

## 🏛️ Architecture

```
microservices-parent/
├── auth-service/                 # Service d'authentification
│   ├── src/
│   │   ├── main/java/com/microservices/auth/
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── service/         # Business Logic
│   │   │   ├── strategy/        # Strategy Pattern
│   │   │   ├── repository/      # Data Access
│   │   │   ├── entity/          # JPA Entities
│   │   │   └── dto/             # Data Transfer Objects
│   │   └── resources/
│   │       └── application.properties
│   ├── Dockerfile
│   └── pom.xml
├── docker-compose.yml            # Orchestration
└── pom.xml                       # Parent POM
```

## 🔧 Services

### 1. Auth Service (Port 8080)

Service d'authentification avec support multi-stratégies :

- **Local** : Username/Email + Password
- **Google OAuth** : Authentification Google
- **GitHub OAuth** : Authentification GitHub

**Endpoints principaux :**
- `POST /api/auth/register` - Inscription
- `POST /api/auth/login` - Connexion
- `POST /api/auth/oauth/login` - Connexion OAuth
- `GET /api/auth/providers` - Liste des providers
- `GET /api/auth/health` - Health check

[📖 Documentation complète](./auth-service/README.md)

### 2. Services Futurs

L'architecture est prête pour l'ajout de :
- **mail-service** : Service d'envoi d'emails
- **notification-service** : Service de notifications
- **nginx** : Reverse proxy et load balancing
- **api-gateway** : Gateway centralisée

## 💻 Technologies

| Technologie | Version | Usage |
|------------|---------|-------|
| Java | 17 | Langage principal |
| Spring Boot | 3.2.0 | Framework |
| Maven | 3.9+ | Build tool |
| H2 Database | Latest | Base de données en mémoire |
| Lombok | 1.18.30 | Réduction du boilerplate |
| Docker | Latest | Conteneurisation |
| Docker Compose | Latest | Orchestration |

## 🚀 Démarrage Rapide

### Prérequis

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (optionnel)

### Option 1 : Maven

```bash
# Depuis la racine du projet
mvn clean install

# Démarrer auth-service
cd auth-service
mvn spring-boot:run
```

### Option 2 : Docker Compose (Recommandé)

```bash
# Depuis la racine du projet
docker-compose up --build
```

Le service sera disponible sur `http://localhost:8080`

## 🧪 Tester l'Application

### H2 Console
Accédez à la console H2 : `http://localhost:8080/h2-console`

**Paramètres :**
- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: _(vide)_

### Exemple cURL

```bash
# 1. Inscription
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securepass123",
    "provider": "LOCAL"
  }'

# 2. Connexion
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "john_doe",
    "password": "securepass123",
    "provider": "LOCAL"
  }'

# 3. Liste des providers
curl http://localhost:8080/api/auth/providers

# 4. Health check
curl http://localhost:8080/api/auth/health
```

### Avec Postman

Importez la collection fournie : `Spring_Auth_API.postman_collection.json`

## 📚 Documentation

- [Guide Complet du Projet](./GUIDE_COMPLET_PROJET.md)
- [Explications Détaillées](./EXPLICATIONS_DETAILLEES.md)
- [Schémas Visuels](./SCHEMAS_VISUELS.md)
- [Q&A Examen](./QUESTIONS_REPONSES_EXAMEN.md)
- [Documentation Auth Service](./auth-service/README.md)

## 🎨 Design Patterns Utilisés

### Strategy Pattern
Le service d'authentification utilise le Strategy Pattern pour gérer différentes méthodes d'authentification de manière extensible :

```java
// Context
AuthenticationService -> utilise -> AuthenticationStrategy

// Stratégies concrètes
LocalAuthenticationStrategy implements AuthenticationStrategy
GoogleAuthenticationStrategy implements AuthenticationStrategy
GitHubAuthenticationStrategy implements AuthenticationStrategy
```

**Avantages :**
- ✅ Facile d'ajouter de nouveaux providers
- ✅ Code découplé et testable
- ✅ Respect du principe Open/Closed
- ✅ Sélection dynamique de la stratégie

## 📦 Ajouter un Nouveau Service

1. Créer un nouveau module dans le dossier racine :
```bash
mkdir new-service
cd new-service
```

2. Créer le `pom.xml` avec le parent :
```xml
<parent>
    <groupId>com.microservices</groupId>
    <artifactId>microservices-parent</artifactId>
    <version>1.0.0</version>
</parent>
```

3. Ajouter le module au `pom.xml` parent :
```xml
<modules>
    <module>auth-service</module>
    <module>new-service</module>
</modules>
```

4. Ajouter le service au `docker-compose.yml`

## 🔒 Sécurité

**⚠️ Note:** Cette implémentation est à but éducatif. Pour la production :

- [ ] Utiliser BCrypt pour hasher les mots de passe
- [ ] Implémenter JWT avec expiration
- [ ] Ajouter HTTPS
- [ ] Implémenter rate limiting
- [ ] Ajouter CORS configuration stricte
- [ ] Utiliser Spring Security complètement
- [ ] Ajouter authentification multi-facteurs
- [ ] Implémenter refresh tokens

## 🐛 Debugging

### Logs
```bash
# Voir les logs du service
docker-compose logs -f auth-service
```

### H2 Console
La console H2 est accessible pour inspecter la base de données en développement.

## 🤝 Contribuer

Ce projet est un exemple éducatif. Les contributions sont les bienvenues pour :
- Ajouter de nouveaux services
- Améliorer la sécurité
- Ajouter des tests
- Améliorer la documentation

## 📝 Licence

Ce projet est à usage éducatif.

## 👨‍💻 Auteur

Projet créé pour démontrer les concepts d'architecture microservices et design patterns en Spring Boot.

## 🔗 Ressources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Microservices Architecture](https://microservices.io/)
- [Docker Documentation](https://docs.docker.com/)
- [Maven Multi-Module](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
