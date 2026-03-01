# 📖 EXPLICATION COMPLÈTE DU PROJET

## 🎯 Vue d'ensemble

Ce projet implémente un système d'authentification complet utilisant **JWT (JSON Web Tokens)** et **Spring Security**. Voici comment tout fonctionne ensemble.

---

## 🏗️ ARCHITECTURE GÉNÉRALE

```
Client (Postman/Frontend)
         ↓
    Controller (Reçoit la requête HTTP)
         ↓
    Service (Logique métier)
         ↓
    Repository (Accès base de données)
         ↓
    Database (H2)
```

---

## 📦 LES COUCHES DE L'APPLICATION

### 1. **Entity (Entité)** - `User.java`

C'est le **modèle de données** qui représente une table dans la base de données.

```java
@Entity  // Dit à Spring : "Crée une table pour cette classe"
@Table(name = "users")  // Nom de la table
public class User {
    @Id  // Clé primaire
    @GeneratedValue  // Auto-incrémentation
    private Long id;
    
    private String username;
    private String email;
    private String password;  // Sera hashé
    private Role role;  // USER ou ADMIN
}
```

**Annotations importantes** :
- `@Entity` : Cette classe = une table
- `@Id` : Clé primaire (identifiant unique)
- `@GeneratedValue` : L'ID s'auto-incrémente (1, 2, 3...)
- `@Column` : Configuration d'une colonne
- `@PrePersist` : Méthode appelée AVANT la sauvegarde
- `@PreUpdate` : Méthode appelée AVANT la mise à jour

**UserDetails** :
```java
implements UserDetails
```
Cette interface de Spring Security permet à notre `User` d'être utilisé par Spring Security pour l'authentification.

---

### 2. **Repository** - `UserRepository.java`

C'est l'interface qui **accède à la base de données**. Spring génère automatiquement le code SQL !

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByEmail(String email);
}
```

**Magie de Spring** :
- `findByUsername` → Spring génère : `SELECT * FROM users WHERE username = ?`
- `existsByEmail` → Spring génère : `SELECT COUNT(*) > 0 FROM users WHERE email = ?`

**Méthodes héritées** (déjà disponibles sans les écrire) :
- `save(user)` : Sauvegarder
- `findById(id)` : Trouver par ID
- `findAll()` : Tout récupérer
- `delete(user)` : Supprimer

---

### 3. **DTO (Data Transfer Objects)**

Ce sont des classes pour **transférer des données** entre le client et le serveur.

#### `RegisterRequest.java` - Données d'inscription
```java
public class RegisterRequest {
    @NotBlank  // Ne peut pas être vide
    @Size(min = 3, max = 50)  // Entre 3 et 50 caractères
    private String username;
    
    @Email  // Doit être un email valide
    private String email;
    
    @Size(min = 6)  // Minimum 6 caractères
    private String password;
}
```

**Pourquoi des DTO ?**
- ✅ Validation des données (avec `@Valid`)
- ✅ Sécurité (on ne renvoie jamais le mot de passe)
- ✅ Flexibilité (on peut changer l'entité sans affecter l'API)

---

### 4. **Service** - Logique métier

#### `AuthService.java`

C'est le **cerveau** de l'authentification.

##### Méthode d'inscription
```java
public AuthResponse register(RegisterRequest request) {
    // 1. Vérifier si le username existe déjà
    if (userRepository.existsByUsername(request.getUsername())) {
        throw new RuntimeException("Username déjà utilisé!");
    }
    
    // 2. Créer un nouvel utilisateur
    User user = new User();
    user.setUsername(request.getUsername());
    
    // 3. IMPORTANT : Hasher le mot de passe
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    
    // 4. Sauvegarder dans la base
    userRepository.save(user);
    
    // 5. Générer un token JWT
    String token = jwtUtils.generateTokenFromUsername(user.getUsername());
    
    // 6. Retourner la réponse
    return new AuthResponse(token, user.getId(), ...);
}
```

**Étapes clés** :
1. **Validation** : Username/email unique ?
2. **Hash du mot de passe** : JAMAIS stocker en clair !
3. **Sauvegarde** : Insertion en base de données
4. **Génération du token** : Pour l'authentification future
5. **Réponse** : Retour du token au client

##### Méthode de connexion
```java
public AuthResponse login(LoginRequest request) {
    // 1. Authentifier l'utilisateur
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getUsername(),
            request.getPassword()
        )
    );
    
    // 2. Si réussi, générer le token
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    // 3. Retourner le token
    return new AuthResponse(jwt, ...);
}
```

**Ce qui se passe en coulisse** :
1. Spring Security vérifie le username
2. Compare le mot de passe entré avec le hash en base
3. Si identique → Authentication réussie
4. Sinon → Exception (erreur 401)

---

### 5. **Security** - Configuration de la sécurité

#### `JwtUtils.java` - Gestion des tokens JWT

##### Qu'est-ce qu'un JWT ?

Un JWT est un token en 3 parties :
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhbGljZSJ9.signature
     HEADER           PAYLOAD          SIGNATURE
```

- **HEADER** : Type de token et algorithme
- **PAYLOAD** : Données (username, rôles, expiration)
- **SIGNATURE** : Vérification de l'authenticité

##### Génération d'un token
```java
public String generateJwtToken(Authentication authentication) {
    UserDetails user = (UserDetails) authentication.getPrincipal();
    
    return Jwts.builder()
        .setSubject(user.getUsername())  // Qui ?
        .setIssuedAt(new Date())  // Quand ?
        .setExpiration(new Date() + 24h)  // Expire quand ?
        .signWith(secretKey)  // Signer avec clé secrète
        .compact();
}
```

##### Validation d'un token
```java
public boolean validateJwtToken(String token) {
    try {
        Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token);  // Parse et vérifie
        return true;
    } catch (Exception e) {
        return false;  // Token invalide ou expiré
    }
}
```

---

#### `AuthTokenFilter.java` - Filtre JWT

C'est un **filtre** qui intercepte CHAQUE requête HTTP.

```java
protected void doFilterInternal(HttpServletRequest request, ...) {
    // 1. Extraire le token du header Authorization
    String jwt = parseJwt(request);  // "Bearer eyJhbGc..."
    
    // 2. Valider le token
    if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
        // 3. Extraire le username
        String username = jwtUtils.getUsernameFromJwtToken(jwt);
        
        // 4. Charger l'utilisateur
        UserDetails user = userDetailsService.loadUserByUsername(username);
        
        // 5. Créer l'authentification
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user, null, user.getAuthorities()
        );
        
        // 6. L'enregistrer dans le contexte de sécurité
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    
    // 7. Continuer la chaîne de filtres
    filterChain.doFilter(request, response);
}
```

**Flux d'une requête authentifiée** :
```
1. Client envoie : GET /api/auth/profile
   Header: Authorization: Bearer eyJhbG...

2. AuthTokenFilter intercepte
   ↓
3. Extrait le token
   ↓
4. Valide le token
   ↓
5. Charge l'utilisateur
   ↓
6. Met l'user dans le contexte de sécurité
   ↓
7. Controller peut maintenant accéder à l'utilisateur connecté
```

---

#### `SecurityConfig.java` - Configuration principale

C'est ici qu'on configure **qui peut accéder à quoi**.

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http
        .csrf(csrf -> csrf.disable())  // Désactivé pour API REST
        
        .sessionManagement(session -> 
            session.sessionCreationPolicy(STATELESS))  // Pas de sessions
        
        .authorizeHttpRequests(auth -> auth
            // Endpoints publics (pas d'authentification requise)
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/api/public/**").permitAll()
            
            // Tous les autres = authentification requise
            .anyRequest().authenticated()
        )
        
        // Ajouter le filtre JWT
        .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
    
    return http.build();
}
```

**Concepts clés** :

1. **CSRF désactivé** : 
   - CSRF = Cross-Site Request Forgery
   - Pas nécessaire pour une API REST (pas de cookies de session)

2. **STATELESS** :
   - Pas de sessions côté serveur
   - Chaque requête est indépendante
   - L'authentification se fait via le token JWT

3. **Autorisation** :
   - `permitAll()` : Accessible à tous
   - `authenticated()` : Nécessite d'être connecté
   - `hasRole("ADMIN")` : Nécessite le rôle ADMIN

---

### 6. **Controller** - Points d'entrée API

#### `AuthController.java`

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(201).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserProfile() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
}
```

**Annotations importantes** :

- `@RestController` : Cette classe gère des requêtes HTTP et renvoie du JSON
- `@RequestMapping("/api/auth")` : Préfixe pour tous les endpoints
- `@PostMapping("/register")` : Endpoint POST sur /api/auth/register
- `@Valid` : Active la validation des données
- `@RequestBody` : Les données viennent du corps de la requête (JSON)
- `@PreAuthorize` : Vérification de sécurité avant d'exécuter la méthode

---

## 🔄 FLUX COMPLET D'UNE AUTHENTIFICATION

### Scénario 1 : Inscription

```
1. Client → POST /api/auth/register
   Body: { "username": "alice", "email": "...", "password": "..." }

2. AuthController.register() reçoit la requête
   ↓
3. @Valid valide les données (email correct, password > 6 caractères, etc.)
   ↓
4. AuthService.register() est appelé
   ↓
5. Vérifie si username/email existe déjà
   ↓
6. Hash le mot de passe avec BCrypt
   ↓
7. Sauvegarde l'utilisateur dans la base (UserRepository)
   ↓
8. Génère un token JWT (JwtUtils)
   ↓
9. Retourne AuthResponse avec le token
   ↓
10. Client reçoit le token et peut maintenant l'utiliser
```

### Scénario 2 : Connexion

```
1. Client → POST /api/auth/login
   Body: { "username": "alice", "password": "..." }

2. AuthController.login() reçoit la requête
   ↓
3. AuthService.login() est appelé
   ↓
4. AuthenticationManager vérifie les identifiants
   ├─ Charge l'utilisateur (UserDetailsService)
   ├─ Compare le password avec le hash en base
   └─ Si identique → Authentication réussie
   ↓
5. Génère un token JWT
   ↓
6. Retourne le token au client
```

### Scénario 3 : Requête authentifiée

```
1. Client → GET /api/auth/profile
   Header: Authorization: Bearer eyJhbG...

2. AuthTokenFilter intercepte AVANT le controller
   ↓
3. Extrait le token du header
   ↓
4. Valide le token (JwtUtils)
   ↓
5. Extrait le username du token
   ↓
6. Charge l'utilisateur complet (UserDetailsService)
   ↓
7. Met l'utilisateur dans SecurityContext
   ↓
8. AuthController.getUserProfile() est appelé
   ↓
9. Récupère l'utilisateur actuel du contexte
   ↓
10. Retourne les données de l'utilisateur
```

---

## 🔐 SÉCURITÉ

### 1. Hash des mots de passe

**JAMAIS stocker les mots de passe en clair !**

```java
// ❌ MAUVAIS
user.setPassword(request.getPassword());

// ✅ BON
user.setPassword(passwordEncoder.encode(request.getPassword()));
```

**BCrypt** :
- Algorithme de hash one-way (irreversible)
- Ajoute un "salt" aléatoire
- Même mot de passe = hashs différents !

Exemple :
```
password123 → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
password123 → $2a$10$DifferentHashCarSaltDifferent...
```

### 2. Validation des données

```java
@NotBlank(message = "Le nom d'utilisateur est obligatoire")
@Size(min = 3, max = 50)
private String username;

@Email(message = "Email invalide")
private String email;
```

Empêche :
- Données vides
- Formats invalides
- Injections SQL
- XSS

### 3. Protection des endpoints

```java
// Accessible sans authentification
.requestMatchers("/api/public/**").permitAll()

// Authentification requise
.anyRequest().authenticated()

// Rôle spécifique requis
@PreAuthorize("hasRole('ADMIN')")
```

---

## 💡 CONCEPTS SPRING

### 1. Injection de dépendances

```java
@Autowired
private UserRepository userRepository;
```

**Sans Spring** :
```java
UserRepository userRepository = new UserRepository();
```

**Avec Spring** :
- Spring crée automatiquement l'instance
- Spring gère le cycle de vie
- Un seul objet réutilisé partout (Singleton)

### 2. Annotations de stéréotype

- `@Component` : Composant Spring générique
- `@Service` : Logique métier
- `@Repository` : Accès aux données
- `@Controller` / `@RestController` : Gestion des requêtes HTTP
- `@Configuration` : Configuration Spring

### 3. Transactions

```java
@Transactional
public void saveUser(User user) {
    userRepository.save(user);
}
```

Si une erreur se produit → Rollback automatique !

---

## 🎓 POUR DÉBUGGER

### 1. Activer les logs

Dans `application.properties` :
```properties
logging.level.com.auth=DEBUG
logging.level.org.springframework.security=DEBUG
```

### 2. Tester 
avec Postman

1. Inscrivez-vous : POST /api/auth/register
2. Copiez le token de la réponse
3. Utilisez-le dans les requêtes suivantes :
   - Authorization: Bearer <token>

### 3. Console H2

- URL : http://localhost:8080/h2-console
- Voir les utilisateurs : `SELECT * FROM users;`

---

## 📚 RÉSUMÉ DES CONCEPTS

| Concept | Rôle |
|---------|------|
| **Entity** | Modèle de données (table) |
| **Repository** | Accès base de données |
| **Service** | Logique métier |
| **Controller** | Endpoints HTTP |
| **DTO** | Transfert de données |
| **JWT** | Token d'authentification |
| **Filter** | Intercepte les requêtes |
| **Security Config** | Configure qui peut accéder à quoi |
| **BCrypt** | Hash des mots de passe |
| **Spring Security** | Framework de sécurité |

---

## 🚀 PROCHAINE ÉTAPE : NOTIFICATIONS

Pour implémenter les notifications, nous allons créer :

1. **Entity Notification** : Modèle de notification
2. **Repository** : NotificationRepository
3. **Service** : NotificationService
4. **Controller** : NotificationController

Les notifications seront liées aux utilisateurs (relation OneToMany).

**Dites-moi quand vous êtes prêt pour continuer !** 🎯
