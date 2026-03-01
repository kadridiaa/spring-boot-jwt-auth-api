# 🔐 Application d'Authentification Spring Boot

Application complète d'authentification avec Spring Boot 3, Spring Security et JWT.

## 📋 Fonctionnalités

- ✅ **Inscription** d'utilisateurs avec validation
- ✅ **Connexion** avec génération de token JWT
- ✅ **Authentification** basée sur JWT
- ✅ **Autorisation** par rôles (USER, ADMIN)
- ✅ **Sécurisation** des endpoints
- ✅ **Base de données** H2 en mémoire
- ✅ **Validation** des données
- ✅ **Gestion des erreurs**

## 🛠️ Technologies utilisées

- **Spring Boot 3.2.0** - Framework principal
- **Spring Security** - Sécurité et authentification
- **Spring Data JPA** - Persistance des données
- **JWT (JSON Web Token)** - Tokens d'authentification
- **H2 Database** - Base de données en mémoire
- **Lombok** - Réduction du code boilerplate
- **Maven** - Gestion des dépendances

## 🚀 Démarrage rapide

### Prérequis

- Java 17 ou supérieur
- Maven 3.6+

### Installation

1. **Cloner ou télécharger le projet**

2. **Ouvrir le terminal dans le dossier du projet**

3. **Compiler le projet** :
```bash
mvn clean install
```

4. **Lancer l'application** :
```bash
mvn spring-boot:run
```

L'application démarre sur **http://localhost:8080**

### Console H2

Accédez à la console H2 pour voir la base de données :
- URL : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:authdb`
- Username : `sa`
- Password : (laisser vide)

## 📚 Endpoints disponibles

### Endpoints publics (sans authentification)

#### 1. Vérifier le statut de l'application
```http
GET http://localhost:8080/api/public/health
```

#### 2. Message de bienvenue
```http
GET http://localhost:8080/api/public/hello
```

#### 3. Inscription
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "alice",
  "email": "alice@example.com",
  "password": "password123"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER",
  "message": "Utilisateur enregistré avec succès !"
}
```

#### 4. Connexion
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "alice",
  "password": "password123"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "alice",
  "email": "alice@example.com",
  "role": "USER"
}
```

### Endpoints protégés (nécessitent un token JWT)

Pour accéder aux endpoints protégés, ajoutez le header :
```
Authorization: Bearer <votre_token_jwt>
```

#### 5. Profil utilisateur
```http
GET http://localhost:8080/api/auth/profile
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### 6. Test utilisateur (USER ou ADMIN)
```http
GET http://localhost:8080/api/auth/user/test
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

#### 7. Test admin (seulement ADMIN)
```http
GET http://localhost:8080/api/auth/admin/test
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

## 👥 Utilisateurs de test

Au démarrage de l'application, deux utilisateurs sont créés automatiquement :

| Username | Password     | Rôle  | Email              |
|----------|--------------|-------|--------------------|
| user     | password123  | USER  | user@example.com   |
| admin    | admin123     | ADMIN | admin@example.com  |

## 🧪 Tester avec Postman

1. **Importer** les requêtes ci-dessus dans Postman

2. **Connexion** :
   - Faites une requête POST à `/api/auth/login` avec user/password123
   - Copiez le token de la réponse

3. **Utiliser le token** :
   - Dans Postman, allez dans l'onglet "Authorization"
   - Choisissez "Bearer Token"
   - Collez votre token
   - Testez les endpoints protégés

## 📁 Structure du projet

```
src/main/java/com/auth/
├── SpringAuthApplication.java          # Classe principale
├── config/
│   ├── SecurityConfig.java            # Configuration Spring Security
│   └── DataInitializer.java           # Données de test
├── controller/
│   ├── AuthController.java            # Endpoints d'authentification
│   └── PublicController.java          # Endpoints publics
├── dto/
│   ├── RegisterRequest.java           # DTO inscription
│   ├── LoginRequest.java              # DTO connexion
│   ├── AuthResponse.java              # DTO réponse auth
│   └── MessageResponse.java           # DTO message
├── entity/
│   └── User.java                      # Entité utilisateur
├── repository/
│   └── UserRepository.java            # Repository utilisateur
├── security/
│   ├── JwtUtils.java                  # Utilitaire JWT
│   ├── AuthTokenFilter.java          # Filtre de validation JWT
│   └── AuthEntryPointJwt.java         # Gestion des erreurs auth
└── service/
    ├── AuthService.java               # Service d'authentification
    └── UserDetailsServiceImpl.java   # Service UserDetails
```

## 🔑 Concepts clés

### 1. **JWT (JSON Web Token)**
- Token généré après connexion réussie
- Contient les informations de l'utilisateur
- Utilisé pour authentifier les requêtes suivantes
- Expire après 24 heures (configurable)

### 2. **Spring Security**
- Gère l'authentification et l'autorisation
- Protège les endpoints
- Hash les mots de passe avec BCrypt

### 3. **Architecture**
- **Controller** : Reçoit les requêtes HTTP
- **Service** : Logique métier
- **Repository** : Accès aux données
- **Entity** : Modèle de données
- **DTO** : Transfert de données
- **Security** : Sécurité et JWT

## 🔒 Sécurité

- Mots de passe hashés avec **BCrypt**
- Protection **CSRF** désactivée (API REST)
- Sessions **STATELESS** (JWT)
- Validation des données d'entrée
- Gestion des erreurs sécurisée

## ⚙️ Configuration

Modifiez `application.properties` pour :
- Changer le port : `server.port=8081`
- Modifier la clé JWT : `jwt.secret=VotreNouvelleCle`
- Changer la durée du token : `jwt.expiration=3600000` (1 heure)

## 📝 Prochaines étapes

### Pour ajouter des notifications :
1. Créer une entité `Notification`
2. Créer un service `NotificationService`
3. Créer un contrôleur `NotificationController`
4. Lier les notifications aux utilisateurs

## 🐛 Résolution de problèmes

### Erreur "Port 8080 déjà utilisé"
Changez le port dans `application.properties` :
```properties
server.port=8081
```

### Erreur JWT
Vérifiez que :
- Le token est bien dans le header `Authorization: Bearer <token>`
- Le token n'est pas expiré
- La clé secrète est correcte

### Base de données
La base H2 se réinitialise à chaque redémarrage. Pour persister :
```properties
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:h2:file:./data/authdb
```

## 📞 Aide

Pour toute question, vérifiez :
- Les logs dans la console
- La console H2 pour l'état de la base de données
- Les messages d'erreur détaillés

---

**Bon apprentissage avec Spring Boot ! 🚀**
