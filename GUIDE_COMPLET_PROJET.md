# 📚 GUIDE COMPLET DU PROJET - API d'Authentification Spring Boot

## 🎯 Vue d'ensemble du projet

**Nom** : Spring Authentication App  
**Type** : API REST avec authentification JWT  
**Techno** : Spring Boot 3.2.0 + Spring Security + JWT + MySQL  
**But** : Système d'authentification sécurisé avec gestion des utilisateurs et des rôles

---

## 🏗️ ARCHITECTURE DU PROJET

### Structure des packages (MVC + Couche sécurité)

```
com.auth/
├── SpringAuthApplication.java     ← Point d'entrée
├── config/                        ← Configuration
│   ├── SecurityConfig.java        ← Configuration Spring Security
│   └── DataInitializer.java       ← Données de test au démarrage
├── controller/                    ← Contrôleurs REST (API endpoints)
│   ├── AuthController.java        ← Login, Register, Profile
│   └── PublicController.java      ← Endpoints publics
├── dto/                           ← Data Transfer Objects (requêtes/réponses)
│   ├── LoginRequest.java
│   ├── RegisterRequest.java
│   ├── AuthResponse.java
│   └── MessageResponse.java
├── entity/                        ← Entités JPA (tables BDD)
│   └── User.java                  ← Utilisateur (table users)
├── repository/                    ← Accès aux données (JPA)
│   └── UserRepository.java
├── security/                      ← Logique de sécurité JWT
│   ├── JwtUtils.java              ← Génération/validation tokens
│   ├── AuthTokenFilter.java      ← Filtre qui vérifie le token
│   └── AuthEntryPointJwt.java     ← Gestion erreurs auth
└── service/                       ← Logique métier
    ├── AuthService.java           ← Services login/register
    └── UserDetailsServiceImpl.java ← Chargement utilisateur pour Spring Security
```

---

## 📁 EXPLICATION FICHIER PAR FICHIER

### 1️⃣ **pom.xml** - Configuration Maven

**Rôle** : Définit les dépendances et la configuration du projet

**Dépendances clés :**
```xml
- spring-boot-starter-web        → API REST
- spring-boot-starter-security   → Sécurité
- spring-boot-starter-data-jpa   → Accès base de données
- mysql-connector-j              → Driver MySQL
- jjwt (3 libs)                  → Génération/validation JWT
- lombok                         → Réduction code (getters/setters auto)
```

**Questions prof :**
- **Q: Pourquoi Spring Boot ?**
  R: Framework qui simplifie la configuration, auto-configuration, serveur embarqué (Tomcat)
  
- **Q: Pourquoi JPA ?**
  R: Abstraction de la BDD, pas besoin d'écrire du SQL, gestion automatique des tables

---

### 2️⃣ **application.properties** - Configuration application

```properties
# Port serveur
server.port=8080

# Base de données MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/authdb
spring.datasource.username=root
spring.datasource.password=root123

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update  ← Création/MAJ auto des tables
spring.jpa.show-sql=true              ← Affiche les requêtes SQL

# JWT
jwt.secret=VotreCleSecrete...         ← Clé de signature (512 bits min)
jwt.expiration=86400000               ← Durée validité token (24h)
```

**Questions prof :**
- **Q: C'est quoi ddl-auto=update ?**
  R: Hibernate crée/met à jour automatiquement les tables en fonction des entités Java
  
- **Q: Pourquoi 86400000 ?**
  R: C'est 24h en millisecondes (24 × 60 × 60 × 1000)

---

### 3️⃣ **SpringAuthApplication.java** - Point d'entrée

```java
@SpringBootApplication  ← Active auto-config + component scan
public class SpringAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(...);  ← Démarre l'application
    }
}
```

**Questions prof :**
- **Q: Qu'est-ce que @SpringBootApplication fait ?**
  R: C'est une méta-annotation = @Configuration + @EnableAutoConfiguration + @ComponentScan
  - @Configuration : permet de définir des beans
  - @EnableAutoConfiguration : config auto selon les dépendances
  - @ComponentScan : scan les @Component, @Service, @Repository, @Controller

---

### 4️⃣ **User.java** - Entité utilisateur

```java
@Entity                           ← Entité JPA
@Table(name = "users")            ← Nom de la table MySQL
public class User implements UserDetails {  ← Interface Spring Security
    
    @Id @GeneratedValue           ← Clé primaire auto-incrémentée
    private Long id;
    
    @Column(unique = true)        ← Contrainte d'unicité
    private String username;
    
    @Email                        ← Validation email
    private String email;
    
    private String password;      ← Mot de passe hashé (BCrypt)
    
    @Enumerated(EnumType.STRING)  ← Enum stocké comme STRING en BDD
    private Role role;            ← USER ou ADMIN
    
    // Implémente UserDetails pour Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
```

**Questions prof :**
- **Q: Pourquoi implements UserDetails ?**
  R: Interface requise par Spring Security pour représenter un utilisateur authentifié
  
- **Q: Comment le mot de passe est stocké ?**
  R: Hashé avec BCrypt (algorithme de hachage sécurisé avec salt automatique)

- **Q: C'est quoi GrantedAuthority ?**
  R: Représente un rôle/permission dans Spring Security (ex: ROLE_USER, ROLE_ADMIN)

---

### 5️⃣ **UserRepository.java** - Accès données

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
}
```

**Questions prof :**
- **Q: Pourquoi juste une interface ?**
  R: Spring Data JPA génère automatiquement l'implémentation à partir du nom de la méthode
  
- **Q: Comment ça marche findByUsername ?**
  R: Naming convention : `findBy` + nom du champ → génère `SELECT * FROM users WHERE username = ?`

---

### 6️⃣ **SecurityConfig.java** - Configuration sécurité

**Rôle** : Configure Spring Security et la chaîne de filtres

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  ← Active @PreAuthorize sur les méthodes
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable())  ← Désactive CSRF (API REST)
            .sessionManagement(SessionCreationPolicy.STATELESS)  ← Pas de session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  ← Hash des mots de passe
    }
}
```

**Questions prof :**
- **Q: Pourquoi STATELESS ?**
  R: Pas de session côté serveur, tout est dans le token JWT → scalabilité

- **Q: Pourquoi désactiver CSRF ?**
  R: CSRF protège contre les attaques via formulaires. Pas nécessaire pour API REST avec JWT

- **Q: C'est quoi le filtre ?**
  R: AuthTokenFilter intercepte CHAQUE requête, extrait le JWT, et valide l'authentification

---

### 7️⃣ **JwtUtils.java** - Gestion JWT

**Rôle** : Créer, valider, et extraire les infos des tokens JWT

```java
@Component
public class JwtUtils {
    
    // Génère un token JWT
    public String generateJwtToken(Authentication auth) {
        return Jwts.builder()
            .setSubject(username)           ← Qui est l'utilisateur
            .setIssuedAt(new Date())        ← Quand créé
            .setExpiration(new Date(...))   ← Quand expire
            .signWith(key, HS512)           ← Signature avec clé secrète
            .compact();                     ← Génère le token
    }
    
    // Extrait le username du token
    public String getUsernameFromJwtToken(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)          ← Vérifie signature + parse
            .getBody()
            .getSubject();                  ← Récupère le username
    }
    
    // Valide le token
    public boolean validateJwtToken(String token) {
        try {
            Jwts.parserBuilder()...parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;  ← Token invalide/expiré
        }
    }
}
```

**Questions prof :**
- **Q: Structure d'un JWT ?**
  R: 3 parties séparées par des points : `Header.Payload.Signature`
  - Header : algorithme + type (HS512, JWT)
  - Payload : données (username, expiration)
  - Signature : garantit l'intégrité

- **Q: Pourquoi HS512 ?**
  R: Algorithme de signature symétrique (HMAC avec SHA-512), sécurisé et rapide

---

### 8️⃣ **AuthTokenFilter.java** - Filtre JWT

**Rôle** : Intercepte chaque requête HTTP, vérifie le token, authentifie l'utilisateur

```java
@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        try {
            // 1. Extraire le token du header Authorization
            String jwt = parseJwt(request);  ← Récupère "Bearer xxx"
            
            // 2. Valider le token
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                
                // 3. Extraire le username
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                
                // 4. Charger l'utilisateur depuis la BDD
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // 5. Créer l'objet d'authentification
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                    );
                
                // 6. Mettre l'utilisateur dans le contexte Spring Security
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Erreur authentification: {}", e.getMessage());
        }
        
        // 7. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);  ← Enlève "Bearer "
        }
        return null;
    }
}
```

**Questions prof :**
- **Q: Pourquoi OncePerRequestFilter ?**
  R: Garantit que le filtre s'exécute UNE SEULE FOIS par requête

- **Q: C'est quoi SecurityContextHolder ?**
  R: Stockage thread-local de l'authentification actuelle (accessible partout dans la requête)

---

### 9️⃣ **AuthService.java** - Logique métier

```java
@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    // Inscription
    public AuthResponse register(RegisterRequest request) {
        // 1. Vérifier si username/email existe déjà
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username déjà utilisé");
        }
        
        // 2. Créer l'utilisateur
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  ← Hash
        user.setRole(Role.USER);
        
        // 3. Sauvegarder en BDD
        userRepository.save(user);
        
        // 4. Générer le token JWT
        String token = jwtUtils.generateTokenFromUsername(user.getUsername());
        
        // 5. Retourner la réponse
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
    
    // Connexion
    public AuthResponse login(LoginRequest request) {
        // 1. Authentifier avec Spring Security
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );  ← Lance exception si mauvais mot de passe
        
        // 2. Mettre dans le contexte
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 3. Générer le token
        String token = jwtUtils.generateJwtToken(authentication);
        
        // 4. Récupérer l'utilisateur
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        
        // 5. Retourner la réponse
        return new AuthResponse(token, user.getUsername(), user.getEmail(), user.getRole());
    }
}
```

**Questions prof :**
- **Q: Pourquoi encoder le mot de passe ?**
  R: Sécurité ! Jamais stocker en clair. BCrypt ajoute un salt aléatoire → même mot de passe = hash différent

- **Q: Comment Spring vérifie le mot de passe ?**
  R: AuthenticationManager utilise le PasswordEncoder pour comparer :
  `passwordEncoder.matches(passwordSaisi, passwordHashéEnBDD)`

---

### 🔟 **AuthController.java** - Endpoints API

```java
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")  ← Autorise CORS (appels depuis le navigateur)
public class AuthController {
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")  ← Nécessite authentification
    public ResponseEntity<?> getUserProfile() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
    
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")  ← Nécessite rôle ADMIN
    public ResponseEntity<?> adminTest() {
        return ResponseEntity.ok(new MessageResponse("Accès admin réussi"));
    }
}
```

**Questions prof :**
- **Q: C'est quoi @Valid ?**
  R: Valide automatiquement les contraintes (@NotBlank, @Email...) du DTO

- **Q: Comment fonctionne @PreAuthorize ?**
  R: Vérifie l'expression AVANT d'exécuter la méthode. Si false → 403 Forbidden

---

## 🔄 FLUX D'EXÉCUTION COMPLET

### Scénario 1 : Inscription

```
1. Client → POST /api/auth/register
   Body: { "username": "john", "email": "...", "password": "..." }

2. Spring → AuthController.register()
   - Valide les données (@Valid)

3. AuthController → AuthService.register()
   - Vérifie unicité username/email
   - Hash le mot de passe avec BCrypt
   - Crée User en BDD
   - Génère JWT avec JwtUtils

4. Réponse ← { "token": "eyJ...", "username": "john", ... }
```

### Scénario 2 : Connexion

```
1. Client → POST /api/auth/login
   Body: { "username": "john", "password": "..." }

2. AuthController → AuthService.login()

3. AuthService → AuthenticationManager.authenticate()
   - Utilise UserDetailsServiceImpl pour charger l'utilisateur
   - Compare mot de passe avec PasswordEncoder
   - Si OK : retourne Authentication

4. AuthService → JwtUtils.generateJwtToken()

5. Réponse ← { "token": "eyJ...", ... }
```

### Scénario 3 : Accès route protégée

```
1. Client → GET /api/auth/profile
   Header: Authorization: Bearer eyJ...

2. AuthTokenFilter.doFilterInternal()
   - Extrait le token du header
   - Valide avec JwtUtils.validateJwtToken()
   - Extrait username du token
   - Charge User depuis BDD via UserDetailsService
   - Met l'auth dans SecurityContextHolder

3. Spring Security vérifie @PreAuthorize("isAuthenticated()")
   - OK → continue
   - KO → 403 Forbidden

4. AuthController.getUserProfile()
   - Récupère l'utilisateur depuis SecurityContext

5. Réponse ← User data
```

---

## 🔐 CONCEPTS CLÉS À CONNAÎTRE

### JWT (JSON Web Token)

**Structure :**
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huIiwiaWF0IjoxNzA5...
   │     Header    │    Payload    │   Signature
```

**Avantages :**
- ✅ Stateless (pas de session serveur)
- ✅ Scalable (peut être vérifié par n'importe quel serveur)
- ✅ Auto-contenu (contient toutes les infos)

**Inconvénients :**
- ❌ Impossible de "révoquer" (sauf blacklist)
- ❌ Taille plus grande qu'un ID de session

### BCrypt

**Pourquoi ?**
- Hash irréversible (impossible de retrouver le mot de passe)
- Salt automatique (même mot de passe → hashs différents)
- Lent volontairement (protection contre brute-force)

**Exemple :**
```
"password123" → "$2a$10$EuF.hg8b9Y5xJfv8K7x4M.ZvN..."
"password123" → "$2a$10$DifferentSaltDonneHashDifferent..."
```

### Spring Security - Chaîne de filtres

```
Client Request
    ↓
[CORS Filter]
    ↓
[AuthTokenFilter] ← Notre filtre JWT
    ↓
[UsernamePasswordAuthenticationFilter]
    ↓
[SecurityContextPersistenceFilter]
    ↓
Controller
```

### Annotations importantes

| Annotation | Rôle |
|------------|------|
| `@RestController` | Contrôleur REST (retourne JSON) |
| `@Service` | Service métier |
| `@Repository` | Accès données |
| `@Entity` | Entité JPA (table BDD) |
| `@Configuration` | Classe de configuration Spring |
| `@Component` | Bean Spring générique |
| `@Autowired` | Injection de dépendances |
| `@PreAuthorize` | Contrôle d'accès sur méthode |
| `@Valid` | Validation des données |

---

## 🎓 QUESTIONS FRÉQUENTES DU PROF

### Q1: Pourquoi JWT et pas sessions ?

**R:** 
- **Sessions** : État stocké côté serveur → problème de scalabilité (load balancer)
- **JWT** : Stateless, le serveur ne stocke rien → facilement scalable

### Q2: Comment sécuriser contre le vol de token ?

**R:**
- Utiliser HTTPS (SSL/TLS)
- Courte durée de vie (24h)
- Refresh tokens pour renouveler
- Stocker dans httpOnly cookies (pas localStorage)

### Q3: Différence entre @Component, @Service, @Repository ?

**R:**
- **@Component** : Bean Spring générique
- **@Service** : Logique métier (spécialisation de @Component)
- **@Repository** : Accès données + gestion exceptions JPA (spécialisation de @Component)
Techniquement identiques, mais sémantiquement différents pour clarté.

### Q4: C'est quoi l'injection de dépendances ?

**R:**
Spring crée et injecte automatiquement les objets.
```java
@Autowired
private UserRepository userRepository;  ← Spring injecte automatiquement
```
Avantages : Découplage, testabilité, pas de `new`

### Q5: Comment marche @PreAuthorize ?

**R:**
C'est un proxy AOP (Aspect-Oriented Programming).
Spring crée une classe wrapper qui :
1. Vérifie l'expression SpEL
2. Si OK → appelle la vraie méthode
3. Si KO → lance AccessDeniedException

### Q6: Pourquoi BCrypt et pas SHA256 ?

**R:**
- **SHA256** : Rapide → vulnérable au brute-force
- **BCrypt** : Lent + salt → résiste au brute-force
BCrypt a un "work factor" ajustable pour rester sécurisé face à l'évolution du matériel.

### Q7: C'est quoi JPA / Hibernate ?

**R:**
- **JPA** (Java Persistence API) : Spécification standard Java pour ORM
- **Hibernate** : Implémentation de JPA (la plus populaire)
- **ORM** : Mapping objet ↔ relationnel (classe Java ↔ table SQL)

### Q8: Différence entre @PathVariable et @RequestParam ?

**R:**
```java
@GetMapping("/users/{id}")          // id dans l'URL
void get(@PathVariable Long id)

@GetMapping("/users")               // ?page=1 dans l'URL
void list(@RequestParam int page)
```

### Q9: Comment tester l'API ?

**R:**
- Postman (interface graphique)
- cURL (ligne de commande)
- Interface HTML custom (votre test-interface.html)
- Tests unitaires (JUnit + MockMvc)

### Q10: Quelle est l'architecture utilisée ?

**R:**
**Architecture en couches (Layered Architecture)** :
```
Presentation Layer (Controllers)
      ↓
Business Layer (Services)
      ↓
Persistence Layer (Repositories)
      ↓
Database (MySQL)
```

+ **Security Layer** (transversal) : Filtres, authentification

---

## 🚀 POINTS FORTS À MENTIONNER

1. **Sécurité robuste** :
   - Authentification JWT
   - Mots de passe hashés avec BCrypt
   - Validation des entrées
   - Gestion des erreurs

2. **Architecture propre** :
   - Séparation des responsabilités
   - Code réutilisable
   - Facile à tester

3. **Scalabilité** :
   - Stateless (JWT)
   - Peut ajouter plusieurs serveurs facilement

4. **Technologies modernes** :
   - Spring Boot 3.2
   - Java 17
   - MySQL 8.4

---

## 📝 VOCABULAIRE TECHNIQUE

| Terme | Définition |
|-------|------------|
| **REST API** | Architecture basée sur HTTP (GET, POST, PUT, DELETE) |
| **JWT** | Token d'authentification auto-contenu et signé |
| **BCrypt** | Algorithme de hachage de mots de passe avec salt |
| **ORM** | Mapping objet-relationnel (objet Java ↔ table SQL) |
| **DTO** | Objet de transfert de données (requête/réponse API) |
| **Bean** | Objet géré par Spring (créé et injecté automatiquement) |
| **Filter** | Intercepteur qui traite chaque requête HTTP |
| **CORS** | Mécanisme de sécurité du navigateur (cross-origin) |
| **Stateless** | Sans état côté serveur (tout dans le token) |
| **AOP** | Programmation orientée aspect (cross-cutting concerns) |

---

## ✅ CHECKLIST AVANT DÉMO

- [ ] Expliquer l'architecture globale
- [ ] Montrer le flux d'une requête complète
- [ ] Expliquer JWT et pourquoi on l'utilise
- [ ] Démontrer l'API (Postman ou interface HTML)
- [ ] Expliquer Spring Security et les filtres
- [ ] Montrer la base de données MySQL
- [ ] Expliquer les rôles (USER vs ADMIN)
- [ ] Parler de la sécurité (BCrypt, validation)

---

## 🎯 CONSEIL FINAL

**Le prof va probablement demander :**
1. "Explique-moi le flux d'une requête de login"
2. "Comment fonctionne l'authentification JWT ?"
3. "Pourquoi ce choix d'architecture ?"

**Soyez prêt à dessiner le schéma au tableau !**

Bonne chance ! 🍀
