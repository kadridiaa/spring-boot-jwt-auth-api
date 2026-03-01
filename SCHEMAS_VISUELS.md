# 🎨 SCHÉMAS ET EXEMPLES VISUELS

## 🔄 FLUX COMPLET D'UNE REQUÊTE

### 📝 Scénario : Login puis accès au profil

```
┌─────────────────────────────────────────────────────────────────────┐
│                        1. LOGIN (POST /api/auth/login)               │
└─────────────────────────────────────────────────────────────────────┘

CLIENT                    CONTROLLER              SERVICE                  DATABASE
  │                          │                       │                        │
  │ POST /api/auth/login     │                       │                        │
  │ {"username":"user",      │                       │                        │
  │  "password":"pass123"}   │                       │                        │
  ├─────────────────────────>│                       │                        │
  │                          │ register(request)     │                        │
  │                          ├──────────────────────>│                        │
  │                          │                       │ findByUsername("user") │
  │                          │                       ├───────────────────────>│
  │                          │                       │<───────────────────────┤
  │                          │                       │ User object            │
  │                          │                       │                        │
  │                          │                       │ PasswordEncoder        │
  │                          │                       │ .matches(pass123, hash)│
  │                          │                       │ ✓ OK                   │
  │                          │                       │                        │
  │                          │                       │ JwtUtils               │
  │                          │                       │ .generateToken()       │
  │                          │                       │ → "eyJ..."             │
  │                          │<──────────────────────┤                        │
  │                          │ AuthResponse          │                        │
  │<─────────────────────────┤ {token:"eyJ...",      │                        │
  │ 200 OK                   │  username:"user"}     │                        │
  │                          │                       │                        │


┌─────────────────────────────────────────────────────────────────────┐
│                   2. ACCÈS PROFIL (GET /api/auth/profile)            │
└─────────────────────────────────────────────────────────────────────┘

CLIENT              FILTER                  CONTROLLER           SERVICE
  │                   │                         │                   │
  │ GET /profile      │                         │                   │
  │ Authorization:    │                         │                   │
  │ Bearer eyJ...     │                         │                   │
  ├──────────────────>│                         │                   │
  │                   │ 1. Extrait token        │                   │
  │                   │ 2. Valide signature     │                   │
  │                   │ 3. Vérifie expiration   │                   │
  │                   │ ✓ Token valide          │                   │
  │                   │ 4. Extrait username     │                   │
  │                   │ 5. Charge User depuis   │                   │
  │                   │    BDD                  │                   │
  │                   │ 6. Met dans Security    │                   │
  │                   │    Context              │                   │
  │                   ├─────────────────────────>│                   │
  │                   │                         │ getCurrentUser()  │
  │                   │                         ├──────────────────>│
  │                   │                         │<──────────────────┤
  │                   │                         │ User object       │
  │<──────────────────┴─────────────────────────┤                   │
  │ 200 OK                                      │                   │
  │ {id:1, username:"user", email:"..."}        │                   │
  │                                             │                   │
```

---

## 🏗️ ARCHITECTURE EN COUCHES

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRÉSENTATION LAYER                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  @RestController                                        │   │
│  │  • AuthController    → /api/auth/*                      │   │
│  │  • PublicController  → /api/public/*                    │   │
│  │                                                          │   │
│  │  Reçoit les requêtes HTTP                               │   │
│  │  Retourne les réponses JSON                             │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                       BUSINESS LAYER                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  @Service                                               │   │
│  │  • AuthService           → Logique login/register       │   │
│  │  • UserDetailsService    → Chargement utilisateur       │   │
│  │                                                          │   │
│  │  Contient la logique métier                             │   │
│  │  Valide, transforme, orchestre                          │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                     PERSISTENCE LAYER                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  @Repository                                            │   │
│  │  • UserRepository  → findByUsername(), save()...        │   │
│  │                                                          │   │
│  │  Accès aux données (JPA)                                │   │
│  │  Génère les requêtes SQL                                │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↕
┌─────────────────────────────────────────────────────────────────┐
│                       DATABASE LAYER                            │
│                                                                 │
│           MySQL - Base de données "authdb"                      │
│                  Table: users                                   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

     ┌────────────────────────────────────────────────┐
     │         SECURITY LAYER (Transversal)           │
     │                                                │
     │  • SecurityConfig    → Configuration           │
     │  • AuthTokenFilter   → Vérifie JWT             │
     │  • JwtUtils          → Génère/valide tokens    │
     │                                                │
     │  S'applique à TOUTES les couches              │
     └────────────────────────────────────────────────┘
```

---

## 🔐 STRUCTURE JWT

```
eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIiwiaWF0IjoxNzA5Mjk4NDAwLCJleHAiOjE3MDkzODQ4MDB9.kM8_Sig...
│                                      │                                                                               │
│          HEADER (Base64)             │                    PAYLOAD (Base64)                                           │ SIGNATURE
│                                      │                                                                               │
└──────────────────────────────────────┴───────────────────────────────────────────────────────────────────────────────┴──────────


📦 HEADER (décodé)
{
  "alg": "HS512",        ← Algorithme de signature
  "typ": "JWT"           ← Type de token
}

📦 PAYLOAD (décodé)
{
  "sub": "user",         ← Subject (username)
  "iat": 1709298400,     ← Issued At (quand créé)
  "exp": 1709384800      ← Expiration (quand expire)
}

🔒 SIGNATURE
= HMACSHA512(
    base64UrlEncode(header) + "." + base64UrlEncode(payload),
    secret_key
)

Garantit que le token n'a pas été modifié !
```

---

## 🔄 CYCLE DE VIE D'UN MOT DE PASSE

```
┌─────────────────────────────────────────────────────────────────┐
│                    1. INSCRIPTION                               │
└─────────────────────────────────────────────────────────────────┘

   Utilisateur tape        BCrypt encode          Stocké en BDD
   ───────────────        ──────────────         ─────────────
   "password123"    →→→   BCryptPasswordEncoder   "$2a$10$EuF..."
   (clair)                .encode()                (hashé)


┌─────────────────────────────────────────────────────────────────┐
│                    2. CONNEXION                                 │
└─────────────────────────────────────────────────────────────────┘

   User tape              Récupère hash          Comparaison
   ───────────           ──────────────          ────────────
   "password123"         FROM database:          matches(
   (clair)               "$2a$10$EuF..."           "password123",
                         (hashé)                   "$2a$10$EuF..."
                                                 )
                                                   ↓
                                                  TRUE ✓
                                                   ↓
                                              Login réussi !


🔐 Sécurité BCrypt :
───────────────────
• Hash irréversible (impossible de retrouver le mot de passe)
• Salt aléatoire inclus dans le hash
• Même mot de passe → hashs différents
• Coûteux en calcul → résiste au brute-force

Exemple :
"password123" → "$2a$10$AbC..."  (salt: AbC)
"password123" → "$2a$10$XyZ..."  (salt: XyZ)  ← Différent !
```

---

## 🛡️ SPRING SECURITY - CHAÎNE DE FILTRES

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CLIENT REQUEST                                │
│                                                                      │
│        GET /api/auth/profile                                         │
│        Authorization: Bearer eyJhbGci...                             │
└──────────────────────────────────────────────────────────────────────┘
                              ↓
         ┌────────────────────────────────────────┐
         │    1. CORS Filter                      │
         │    Vérifie les origines autorisées     │
         └────────────────────────────────────────┘
                              ↓
         ┌────────────────────────────────────────┐
         │    2. AuthTokenFilter ⭐               │
         │    • Extrait le JWT                    │
         │    • Valide la signature               │
         │    • Vérifie l'expiration              │
         │    • Charge l'utilisateur              │
         │    • Met dans SecurityContext          │
         └────────────────────────────────────────┘
                              ↓
         ┌────────────────────────────────────────┐
         │    3. UsernamePasswordAuthFilter       │
         │    (sauté si JWT valide)               │
         └────────────────────────────────────────┘
                              ↓
         ┌────────────────────────────────────────┐
         │    4. Authorization Filter             │
         │    Vérifie @PreAuthorize               │
         │    hasRole('ADMIN') ?                  │
         └────────────────────────────────────────┘
                              ↓
         ┌────────────────────────────────────────┐
         │         CONTROLLER METHOD              │
         │    @GetMapping("/profile")             │
         │    getUserProfile() { ... }            │
         └────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────────────┐
│                         RESPONSE                                     │
│                                                                      │
│        200 OK                                                        │
│        { "id": 1, "username": "user", ... }                          │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 📊 BASE DE DONNÉES - TABLE USERS

```sql
┌─────────────────────────────────────────────────────────────────────┐
│                          Table: users                               │
├─────┬──────────┬───────────────────────┬──────┬─────────┬─────────┤
│ id  │ username │ email                 │ role │ enabled │ password│
├─────┼──────────┼───────────────────────┼──────┼─────────┼─────────┤
│ 1   │ user     │ user@example.com      │ USER │ true    │ $2a$10..│
│ 2   │ admin    │ admin@example.com     │ ADMIN│ true    │ $2a$10..│
│ 3   │ john     │ john@test.com         │ USER │ true    │ $2a$10..│
└─────┴──────────┴───────────────────────┴──────┴─────────┴─────────┘

Contraintes :
─────────────
• id : PRIMARY KEY, AUTO_INCREMENT
• username : UNIQUE, NOT NULL
• email : UNIQUE, NOT NULL
• password : NOT NULL (hashé avec BCrypt)
• role : ENUM ('USER', 'ADMIN')
• created_at : timestamp (auto)
• updated_at : timestamp (auto)

Créée automatiquement par Hibernate (ddl-auto=update)
```

---

## 🎯 ENDPOINTS DE L'API

```
┌──────────────────────────────────────────────────────────────────────┐
│                      ENDPOINTS PUBLICS                               │
│  (Accessible sans authentification)                                  │
└──────────────────────────────────────────────────────────────────────┘

📗 GET  /api/public/test
   → "API publique accessible à tous"

📗 POST /api/auth/register
   Body: { "username", "email", "password" }
   → { "token", "username", "email", "role" }

📗 POST /api/auth/login
   Body: { "username", "password" }
   → { "token", "username", "email", "role" }


┌──────────────────────────────────────────────────────────────────────┐
│                   ENDPOINTS PROTÉGÉS (JWT requis)                    │
└──────────────────────────────────────────────────────────────────────┘

🔒 GET /api/auth/profile
   Header: Authorization: Bearer <token>
   → Informations de l'utilisateur connecté

🔒 GET /api/auth/user/test
   Header: Authorization: Bearer <token>
   Rôle: USER ou ADMIN
   → Message de test pour utilisateurs

🔒 GET /api/auth/admin/test
   Header: Authorization: Bearer <token>
   Rôle: ADMIN uniquement
   → Message de test pour admins
```

---

## 🧪 EXEMPLES DE REQUÊTES/RÉPONSES

### 1. Inscription

**REQUÊTE :**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "john",
  "email": "john@example.com",
  "password": "securepass123"
}
```

**RÉPONSE (201 Created) :**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huIiwiaWF0IjoxNzA5Mjk...",
  "username": "john",
  "email": "john@example.com",
  "role": "USER"
}
```

---

### 2. Connexion

**REQUÊTE :**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "securepass123"
}
```

**RÉPONSE (200 OK) :**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huIiwiaWF0IjoxNzA5Mjk...",
  "username": "john",
  "email": "john@example.com",
  "role": "USER"
}
```

---

### 3. Profil utilisateur

**REQUÊTE :**
```http
GET http://localhost:8080/api/auth/profile
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqb2huIiwiaWF0...
```

**RÉPONSE (200 OK) :**
```json
{
  "id": 3,
  "username": "john",
  "email": "john@example.com",
  "role": "USER",
  "createdAt": "2026-03-01T14:30:00",
  "enabled": true,
  "accountNonExpired": true,
  "accountNonLocked": true,
  "credentialsNonExpired": true
}
```

---

### 4. Erreur - Token manquant

**REQUÊTE :**
```http
GET http://localhost:8080/api/auth/profile
(pas de header Authorization)
```

**RÉPONSE (403 Forbidden) :**
```json
{
  "error": "Non autorisé",
  "message": "Full authentication is required to access this resource"
}
```

---

### 5. Erreur - Token expiré

**REQUÊTE :**
```http
GET http://localhost:8080/api/auth/profile
Authorization: Bearer <token_expiré>
```

**RÉPONSE (401 Unauthorized) :**
```json
{
  "error": "Non autorisé",
  "message": "Token JWT expiré"
}
```

---

### 6. Erreur - Accès admin refusé

**REQUÊTE :**
```http
GET http://localhost:8080/api/auth/admin/test
Authorization: Bearer <token_user_normal>
```

**RÉPONSE (403 Forbidden) :**
```json
{
  "error": "Accès refusé",
  "message": "Insufficient authorities"
}
```

---

## 🔍 DÉBOGAGE - LOGS À SURVEILLER

```
✅ Démarrage réussi :
───────────────────
Tomcat started on port 8080 (http) with context path ''
Started SpringAuthApplication in 2.996 seconds

✅ Requête SQL (création utilisateur) :
─────────────────────────────────────
Hibernate: insert into users (...) values (?, ?, ?, ...)

✅ Token JWT généré :
────────────────────
Token généré pour user: john

❌ Erreur authentification :
──────────────────────────
Bad credentials
→ Mot de passe incorrect

❌ Token invalide :
─────────────────
Token JWT expiré: JWT expired at 2026-03-01T12:00:00Z
→ Token périmé

❌ Signature invalide :
──────────────────────
Signature JWT invalide: JWT signature does not match
→ Token modifié ou mauvaise clé
```

---

## 🎨 ORGANISATION DU CODE

```
src/main/java/com/auth/
│
├── 📁 config/                    Configuration Spring
│   ├── SecurityConfig.java       → Spring Security (filtres, encodeur...)
│   └── DataInitializer.java      → Données de test au démarrage
│
├── 📁 controller/                Endpoints REST API
│   ├── AuthController.java       → /api/auth/* (login, register, profile)
│   └── PublicController.java     → /api/public/* (test public)
│
├── 📁 dto/                       Objets de transfert (JSON ↔ Java)
│   ├── LoginRequest.java         → { username, password }
│   ├── RegisterRequest.java      → { username, email, password }
│   ├── AuthResponse.java         → { token, username, email, role }
│   └── MessageResponse.java      → { message }
│
├── 📁 entity/                    Entités JPA (tables BDD)
│   └── User.java                 → Table "users"
│
├── 📁 repository/                Accès données (JPA)
│   └── UserRepository.java       → findByUsername(), save(), etc.
│
├── 📁 security/                  Logique de sécurité JWT
│   ├── JwtUtils.java             → Génère/valide tokens JWT
│   ├── AuthTokenFilter.java     → Filtre qui intercepte les requêtes
│   └── AuthEntrPointJwt.java    → Gestion des erreurs d'auth
│
├── 📁 service/                   Logique métier
│   ├── AuthService.java          → Login, register
│   └── UserDetailsServiceImpl... → Charge utilisateur pour Spring Security
│
└── SpringAuthApplication.java    Point d'entrée (main)
```

---

## 💡 ASTUCES POUR L'EXAMEN

### Dessinez au tableau

Soyez prêt à dessiner :
1. **L'architecture en couches** (Controller → Service → Repository → BDD)
2. **Le flux d'une requête** avec les filtres
3. **La structure d'un JWT** (Header.Payload.Signature)

### Vocabulaire à utiliser

- "Architecture RESTful"
- "Stateless authentication"
- "Separation of concerns"
- "Dependency injection"
- "ORM (Object-Relational Mapping)"
- "DTO pattern"

### Anticipez les questions

**"Pourquoi ce choix ?"**
→ Toujours avoir une raison technique (scalabilité, sécurité, maintenabilité)

**"Comment ça marche en interne ?"**
→ Détaillez le mécanisme (filtre, hash, validation...)

**"Quelle alternative ?"**
→ JWT vs Sessions, BCrypt vs SHA256, MySQL vs PostgreSQL

---

Bon courage ! 🚀
