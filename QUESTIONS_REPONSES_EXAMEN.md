# 🎓 QUESTIONS/RÉPONSES TYPE EXAMEN

## ⚡ QUESTIONS RAPIDES (Définitions)

### Q1: C'est quoi Spring Boot ?
**R:** Framework Java qui simplifie le développement d'applications Spring avec :
- Auto-configuration
- Serveur embarqué (Tomcat)
- Gestion des dépendances simplifiée
- Configuration par convention

### Q2: Différence entre @Component, @Service, @Repository ?
**R:**
- `@Component` : Bean Spring générique
- `@Service` : Logique métier (spécialisation sémantique)
- `@Repository` : Accès données + traduction des exceptions JPA
Techniquement identiques, mais clarté sémantique.

### Q3: C'est quoi un JWT ?
**R:** JSON Web Token = token d'authentification en 3 parties :
- Header (algorithme)
- Payload (données utilisateur)
- Signature (garantit intégrité)
Format : `aaaa.bbbb.cccc` (Base64)

### Q4: Pourquoi BCrypt et pas MD5 ?
**R:**
- **MD5** : Rapide, obsolète, vulnérable (rainbow tables)
- **BCrypt** : Lent (résiste brute-force), salt automatique, ajustable

### Q5: C'est quoi STATELESS ?
**R:** Aucun état conservé côté serveur entre les requêtes. Toutes les infos dans le token.
**Avantage** : Scalabilité (load balancing facile)

### Q6: Différence entre @PathVariable et @RequestParam ?
**R:**
```java
@GetMapping("/users/{id}")           // @PathVariable
void get(@PathVariable Long id)       → /users/123

@GetMapping("/users")                 // @RequestParam
void list(@RequestParam int page)     → /users?page=1
```

### Q7: Différence entre @RequestBody et @ResponseBody ?
**R:**
- `@RequestBody` : Désérialise JSON de la requête → objet Java
- `@ResponseBody` : Sérialise objet Java → JSON (auto avec @RestController)

### Q8: C'est quoi JPA ?
**R:** Java Persistence API = spécification ORM standard
- **ORM** : Mapping objet ↔ relationnel (classe ↔ table)
- **Hibernate** : Implémentation de JPA

### Q9: À quoi sert @Autowired ?
**R:** Injection de dépendances. Spring crée et injecte automatiquement l'objet.
Pas besoin de `new`, Spring gère le cycle de vie.

### Q10: Différence entre POST et PUT ?
**R:**
- **POST** : Créer une ressource (non-idempotent)
- **PUT** : Remplacer/modifier une ressource (idempotent)
- **PATCH** : Modification partielle

---

## 🎯 QUESTIONS TECHNIQUES (Compréhension)

### Q11: Explique le cycle de vie d'une requête Spring Boot

**R:**
```
1. Client envoie requête HTTP → localhost:8080/api/auth/login

2. Tomcat (serveur embarqué) reçoit la requête

3. DispatcherServlet (contrôleur frontal Spring MVC)
   │
   ├─> Passe par la chaîne de filtres :
   │   • CORS Filter
   │   • AuthTokenFilter (vérifie JWT)
   │   • Security Filter
   │
   └─> Route vers le bon @Controller selon @RequestMapping

4. Controller reçoit la requête
   • Désérialise JSON → objet Java (@RequestBody)
   • Valide les données (@Valid)

5. Controller appelle le @Service (logique métier)

6. Service appelle le @Repository (accès BDD)

7. Repository utilise JPA/Hibernate
   • Génère et exécute requête SQL
   • Mappe résultat → entité Java

8. Retour en cascade : Repository → Service → Controller

9. Controller sérialise objet Java → JSON (@ResponseBody)

10. Réponse HTTP envoyée au client
```

---

### Q12: Comment fonctionne l'authentification JWT dans ton projet ?

**R:**

**ÉTAPE 1 : Login**
```
1. User POST /api/auth/login { username, password }

2. AuthService.login()
   ├─> AuthenticationManager vérifie les credentials
   │   ├─> UserDetailsService charge User depuis BDD
   │   └─> PasswordEncoder compare les mots de passe
   │
   ├─> Si OK : JwtUtils.generateJwtToken()
   │   └─> Crée JWT avec username, date expiration, signature
   │
   └─> Retourne { token: "eyJ...", username, role }

3. Client stocke le token
```

**ÉTAPE 2 : Requête protégée**
```
1. Client envoie requête avec header:
   Authorization: Bearer eyJ...

2. AuthTokenFilter.doFilterInternal()
   ├─> Extrait token du header
   ├─> JwtUtils.validateJwtToken()
   │   └─> Vérifie signature + expiration
   ├─> JwtUtils.getUsernameFromJwtToken()
   │   └─> Extrait "john" du payload
   ├─> UserDetailsService.loadUserByUsername("john")
   │   └─> Charge User depuis BDD
   └─> Met User dans SecurityContext

3. Spring Security vérifie @PreAuthorize
   └─> Si OK : exécute méthode
       Si KO : 403 Forbidden

4. Controller traite la requête normalement
```

---

### Q13: Pourquoi on hash les mots de passe ? Comment ça marche ?

**R:**

**POURQUOI ?**
- Sécurité : si BDD compromise, attaquant ne peut pas lire les mots de passe
- Conformité : RGPD, ISO 27001

**COMMENT BCRYPT FONCTIONNE ?**

```
┌─────────────────────────────────────────────────────────────┐
│ 1. INSCRIPTION                                              │
└─────────────────────────────────────────────────────────────┘

Input: "password123"
   ↓
BCrypt.encode()
   ├─> Génère salt aléatoire : "eF8k2pQ"
   ├─> Combine : "password123" + "eF8k2pQ"
   ├─> Hash avec Blowfish (rounds = 10)
   └─> Résultat : "$2a$10$eF8k2pQ[...]VxM2p"
                    │  │  │        └─> Hash
                    │  │  └─> Salt
                    │  └─> Coût (2^10 itérations)
                    └─> Version BCrypt

Stocké en BDD : "$2a$10$eF8k2pQ[...]VxM2p"


┌─────────────────────────────────────────────────────────────┐
│ 2. CONNEXION                                                │
└─────────────────────────────────────────────────────────────┘

Input: "password123"
BDD:   "$2a$10$eF8k2pQ[...]VxM2p"
   ↓
PasswordEncoder.matches(input, hashFromDB)
   ├─> Extrait salt du hash : "eF8k2pQ"
   ├─> Hash input avec même salt
   ├─> Compare les deux hashs
   └─> TRUE si identiques

```

**SÉCURITÉ :**
- Même mot de passe → **hashs différents** (salt aléatoire)
- Hash irréversible (impossible de retrouver le mot de passe)
- Coûteux en calcul → résiste au brute-force

---

### Q14: Explique @PreAuthorize et comment Spring Security l'utilise

**R:**

```java
@GetMapping("/admin/test")
@PreAuthorize("hasRole('ADMIN')")  ← Expression SpEL (Spring Expression Language)
public ResponseEntity<?> adminTest() {
    return ResponseEntity.ok("Accès admin");
}
```

**COMMENT ÇA MARCHE ?**

1. **Spring crée un proxy AOP** autour de la méthode
```
           ┌─────────────────────────────┐
 Appel →   │   Proxy (Intercepteur)      │
           │                             │
           │  1. Vérifie @PreAuthorize   │
           │     hasRole('ADMIN') ?      │
           │                             │
           │  2. Récupère l'auth depuis  │
           │     SecurityContext         │
           │                             │
           │  3. Évalue expression SpEL  │
           │     user.authorities        │
           │     contient ROLE_ADMIN ?   │
           │                             │
           │  ┌──── TRUE ────┐           │
           │  │               │           │
           │  ✓            ✗             │
           │  │               │           │
           │  ↓               ↓           │
           │  Exécute      Lance          │
           │  méthode      Access         │
           │  originale    Denied         │
           │               Exception      │
           └─────────────────────────────┘
```

2. **Expressions SpEL disponibles :**
```java
@PreAuthorize("isAuthenticated()")           // Juste connecté
@PreAuthorize("hasRole('ADMIN')")            // Rôle ADMIN
@PreAuthorize("hasRole('USER')")             // Rôle USER
@PreAuthorize("hasAnyRole('USER','ADMIN')")  // USER OU ADMIN
@PreAuthorize("hasAuthority('WRITE')")       // Permission WRITE
@PreAuthorize("#username == authentication.name") // Paramètre = user actuel
```

---

### Q15: Différence entre Session et JWT ?

**R:**

```
┌─────────────────────────────────────────────────────────────┐
│                     SESSION (Stateful)                      │
└─────────────────────────────────────────────────────────────┘

CLIENT                    SERVER
  │                         │
  │ 1. Login                │
  ├────────────────────────>│
  │                         │ Crée session en mémoire
  │                         │ Session ID: abc123
  │                         │ ┌──────────────┐
  │                         │ │ Sessions     │
  │ 2. Cookie: abc123       │ │ abc123 → john│
  │<────────────────────────┤ └──────────────┘
  │                         │
  │ 3. GET /profile         │
  │    Cookie: abc123       │
  ├────────────────────────>│ Cherche abc123 en mémoire
  │                         │ Trouve john
  │                         │
  │ 4. Response             │
  │<────────────────────────┤
  │                         │

AVANTAGES :                INCONVÉNIENTS :
• Facile à révoquer        • État côté serveur
• Contrôle fin             • Problème de scalabilité
                           • Load balancer complexe


┌─────────────────────────────────────────────────────────────┐
│                      JWT (Stateless)                        │
└─────────────────────────────────────────────────────────────┘

CLIENT                    SERVER
  │                         │
  │ 1. Login                │
  ├────────────────────────>│
  │                         │ Génère JWT
  │ 2. JWT: eyJ...          │ (signé avec clé secrète)
  │<────────────────────────┤ Aucune session en mémoire !
  │                         │
  │ 3. GET /profile         │
  │    Bearer: eyJ...       │
  ├────────────────────────>│ Vérifie signature JWT
  │                         │ Extrait username du token
  │                         │ (pas besoin de BDD)
  │ 4. Response             │
  │<────────────────────────┤
  │                         │

AVANTAGES :                INCONVÉNIENTS :
• Stateless                • Difficile à révoquer
• Scalable                 • Taille plus grande
• Décentralisé             • Pas de contrôle fin
```

**QUAND UTILISER QUOI ?**
- **Session** : Application monolithique, besoin de révocation
- **JWT** : API REST, microservices, mobile apps

---

## 🧪 SCÉNARIOS PRATIQUES

### Scénario 1 : "Le token ne marche pas"

**Problème :** Je reçois toujours 403 Forbidden

**DIAGNOSTIC :**
```
1. Vérifier le header Authorization
   ✗ Authorization: eyJ...              ← Manque "Bearer "
   ✓ Authorization: Bearer eyJ...

2. Vérifier la validité du token
   → Vérifier expiration (24h par défaut)
   → Token copié en entier ? (pas de coupure)

3. Vérifier les logs
   → "Token expiré" → regenerer via /login
   → "Signature invalide" → jwt.secret changé ?

4. Tester avec Postman ou cURL
   curl -H "Authorization: Bearer eyJ..." http://localhost:8080/api/auth/profile
```

---

### Scénario 2 : "Les mots de passe ne matchent pas"

**Problème :** Login échoue toujours

**DIAGNOSTIC :**
```
1. Vérifier que le mot de passe est bien hashé en BDD
   SELECT password FROM users WHERE username = 'user';
   → Doit commencer par "$2a$10$..."

2. Vérifier l'encodage lors de l'inscription
   AuthService.register() :
   user.setPassword(passwordEncoder.encode(request.getPassword()));
                    └─> Ne pas oublier !

3. Vérifier la comparaison
   AuthenticationManager utilise bien le PasswordEncoder

4. Test manuel
   String hash = passwordEncoder.encode("password123");
   boolean match = passwordEncoder.matches("password123", hash);
   → Doit être TRUE
```

---

### Scénario 3 : "L'application ne démarre pas"

**Problème :** Erreur au démarrage

**CAUSES FRÉQUENTES :**

```
ERREUR 1 : Port 8080 déjà utilisé
───────────────────────────────────
Web server failed to start. Port 8080 was already in use.

SOLUTION :
• Arrêter le processus : netstat -ano | findstr :8080
• Ou changer le port : server.port=8081


ERREUR 2 : MySQL non démarré
─────────────────────────────
Failed to obtain JDBC Connection

SOLUTION :
• Démarrer MySQL : Start-Process mysqld.exe
• Vérifier connexion : mysql -u root -p


ERREUR 3 : Dépendance manquante
────────────────────────────────
ClassNotFoundException: io.jsonwebtoken.Jwts

SOLUTION :
• mvn clean install
• Vérifier pom.xml (dépendances jjwt)


ERREUR 4 : Clé JWT trop courte
───────────────────────────────
The signing key's size is X bits which is not secure enough

SOLUTION :
• Rallonger jwt.secret (min 512 bits pour HS512)
• Utiliser Keys.secretKeyFor(SignatureAlgorithm.HS512)
```

---

## 📊 COMPARAISON TECHNOLOGIES

### Spring Boot vs Spring "classique"

| Aspect | Spring Boot | Spring classique |
|--------|-------------|------------------|
| Configuration | Auto-configuration | XML/annotations manuelles |
| Serveur | Embarqué (Tomcat) | Externe (déploiement WAR) |
| Dépendances | Starters (tout-en-un) | Manuelles |
| Temps setup | Minutes | Heures |
| Production | Jar exécutable | WAR sur serveur |

---

### MySQL vs H2

| Aspect | MySQL | H2 |
|--------|-------|-----|
| Type | Base relationnelle | Base en mémoire |
| Persistance | Disque | RAM |
| Performance | Optimisé production | Très rapide (RAM) |
| Usage | Production | Développement/tests |
| Installation | Requise | Embarquée |

---

### JWT vs OAuth2

| Aspect | JWT | OAuth2 |
|--------|-----|--------|
| Purpose | Authentification | Autorisation déléguée |
| Complexité | Simple | Complexe |
| Tokens | 1 token | Access + Refresh tokens |
| Use case | API simple | Login tiers (Google, Facebook) |

---

## 🎯 CHECKLIST DÉMONSTRATION

### Avant la démo

- [ ] MySQL démarré (port 3306)
- [ ] Application démarrée (port 8080)
- [ ] Postman ou interface HTML prêt
- [ ] Comptes de test : user/password123, admin/admin123

### Pendant la démo

**1. Montrer l'architecture**
- [ ] Dessiner les couches au tableau
- [ ] Expliquer le rôle de chaque package

**2. Expliquer le flux**
- [ ] Login → génération JWT
- [ ] Requête protégée → validation JWT
- [ ] Contrôle d'accès (USER vs ADMIN)

**3. Démonstration live**
- [ ] Inscription d'un utilisateur
- [ ] Connexion et récupération du token
- [ ] Accès au profil avec le token
- [ ] Test accès admin (échec avec USER, succès avec ADMIN)

**4. Montrer la sécurité**
- [ ] Mot de passe hashé en BDD
- [ ] Token JWT décodé (jwt.io)
- [ ] Erreur si token modifié

**5. Répondre aux questions**
- [ ] Pourquoi ces choix technos ?
- [ ] Alternatives possibles ?
- [ ] Améliorations futures ?

---

## 🚀 AMÉLIORATIONS POSSIBLES (pour impressionner)

### Court terme

1. **Refresh tokens** : Renouveler token sans re-login
2. **Rate limiting** : Limiter nombre de requêtes par IP
3. **Logging** : Logger toutes les tentatives d'auth
4. **Tests unitaires** : JUnit + Mockito

### Moyen terme

5. **Email de confirmation** : Vérifier email à l'inscription
6. **Réinitialisation mot de passe** : Via email
7. **2FA (Two-Factor Auth)** : SMS ou TOTP
8. **Rôles granulaires** : Permissions fines (READ, WRITE, DELETE)

### Long terme

9. **OAuth2** : Login avec Google/GitHub
10. **Microservices** : Séparer auth en service dédié
11. **Redis** : Cache pour tokens blacklistés
12. **Docker** : Containerisation de l'app

---

## 💬 PHRASES CLÉS À UTILISER

- "L'architecture est basée sur le pattern **MVC étendu** avec une couche de sécurité transversale"
- "J'ai utilisé **JWT pour l'authentification stateless** afin de faciliter la scalabilité"
- "Les mots de passe sont hashés avec **BCrypt** qui inclut un salt aléatoire"
- "Spring Security utilise une **chaîne de filtres** pour intercepter et sécuriser chaque requête"
- "Le pattern **DTO** permet de découpler l'API de la structure interne des entités"
- "L'injection de dépendances permet un **couplage faible** et facilite les tests"

---

Vous êtes prêt ! 💪 Bonne chance ! 🍀
