# 🎨 Strategy Design Pattern - Explication Détaillée

## 📚 Qu'est-ce que le Strategy Pattern ?

Le **Strategy Pattern** est un patron de conception comportemental qui permet de définir une famille d'algorithmes, de les encapsuler et de les rendre interchangeables. Il permet à l'algorithme de varier indépendamment des clients qui l'utilisent.

## 🎯 Problème Résolu

Sans Strategy Pattern, pour gérer plusieurs méthodes d'authentification, on aurait un code comme ceci :

```java
public AuthResponse authenticate(LoginRequest request) {
    if (request.getProvider() == AuthProvider.LOCAL) {
        // Code pour authentification locale
        // ... 50 lignes de code ...
    } else if (request.getProvider() == AuthProvider.GOOGLE) {
        // Code pour Google OAuth
        // ... 50 lignes de code ...
    } else if (request.getProvider() == AuthProvider.GITHUB) {
        // Code pour GitHub OAuth
        // ... 50 lignes de code ...
    } else if (request.getProvider() == AuthProvider.FACEBOOK) {
        // Code pour Facebook OAuth
        // ... 50 lignes de code ...
    }
    // Difficile à maintenir et à tester !
}
```

**Problèmes :**
- ❌ Code difficile à lire et à maintenir
- ❌ Violation du principe Open/Closed
- ❌ Tests unitaires complexes
- ❌ Difficile d'ajouter un nouveau provider
- ❌ Duplication de code

## ✅ Solution avec Strategy Pattern

### 1. Interface Strategy

```java
public interface AuthenticationStrategy {
    AuthResponse authenticate(LoginRequest loginRequest);
    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse authenticateOAuth(OAuthLoginRequest oAuthRequest);
    boolean supports(String provider);
}
```

### 2. Stratégies Concrètes

```java
@Component
public class LocalAuthenticationStrategy implements AuthenticationStrategy {
    @Override
    public AuthResponse authenticate(LoginRequest request) {
        // Logique spécifique à l'authentification locale
    }
    
    @Override
    public boolean supports(String provider) {
        return "LOCAL".equalsIgnoreCase(provider);
    }
}

@Component
public class GoogleAuthenticationStrategy implements AuthenticationStrategy {
    @Override
    public AuthResponse authenticateOAuth(OAuthLoginRequest request) {
        // Logique spécifique à Google OAuth
    }
    
    @Override
    public boolean supports(String provider) {
        return "GOOGLE".equalsIgnoreCase(provider);
    }
}

@Component
public class GitHubAuthenticationStrategy implements AuthenticationStrategy {
    @Override
    public AuthResponse authenticateOAuth(OAuthLoginRequest request) {
        // Logique spécifique à GitHub OAuth
    }
    
    @Override
    public boolean supports(String provider) {
        return "GITHUB".equalsIgnoreCase(provider);
    }
}
```

### 3. Context (Service qui utilise les Strategies)

```java
@Service
public class AuthenticationService {
    private final List<AuthenticationStrategy> strategies;
    
    public AuthenticationService(List<AuthenticationStrategy> strategies) {
        this.strategies = strategies;
    }
    
    public AuthResponse authenticate(LoginRequest request) {
        AuthenticationStrategy strategy = selectStrategy(request.getProvider().name());
        return strategy.authenticate(request);
    }
    
    private AuthenticationStrategy selectStrategy(String provider) {
        return strategies.stream()
            .filter(strategy -> strategy.supports(provider))
            .findFirst()
            .orElse(null);
    }
}
```

## 🏗️ Architecture Visuelle

```
┌─────────────────────────────────────────────────────────┐
│                   AuthController                         │
│                  (REST Endpoints)                        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              AuthenticationService                       │
│                   (Context)                              │
│  - Contient List<AuthenticationStrategy>                │
│  - Sélectionne la bonne stratégie                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────┐
        │ AuthenticationStrategy │  ◄───── Interface
        │      (Interface)       │
        └────────────┬───────────┘
                     │
         ┌───────────┼───────────┐
         ▼           ▼           ▼
    ┌────────┐  ┌────────┐  ┌────────┐
    │ Local  │  │ Google │  │ GitHub │
    │Strategy│  │Strategy│  │Strategy│
    └────────┘  └────────┘  └────────┘
```

## 💡 Avantages du Strategy Pattern

### 1. Open/Closed Principle ✅
- Ouvert pour l'extension : Ajouter un nouveau provider est facile
- Fermé pour la modification : Pas besoin de modifier le code existant

```java
// Ajouter Facebook OAuth ? Juste créer une nouvelle class !
@Component
public class FacebookAuthenticationStrategy implements AuthenticationStrategy {
    // Nouvelle implémentation
}
// C'est tout ! Pas besoin de modifier AuthenticationService
```

### 2. Single Responsibility Principle ✅
Chaque stratégie a une seule responsabilité :
- `LocalAuthenticationStrategy` : Gérer l'auth locale
- `GoogleAuthenticationStrategy` : Gérer Google OAuth
- `GitHubAuthenticationStrategy` : Gérer GitHub OAuth

### 3. Testabilité ✅
Tests unitaires faciles pour chaque stratégie :

```java
@Test
void testLocalAuthentication() {
    LocalAuthenticationStrategy strategy = new LocalAuthenticationStrategy(userRepository);
    LoginRequest request = new LoginRequest("user", "pass", AuthProvider.LOCAL);
    AuthResponse response = strategy.authenticate(request);
    assertTrue(response.isSuccess());
}
```

### 4. Maintenabilité ✅
- Code organisé et lisible
- Facile de comprendre et modifier
- Pas de méga-classe avec des if/else

### 5. Runtime Selection ✅
La stratégie est sélectionnée dynamiquement :

```java
// L'utilisateur choisit son provider
POST /api/auth/login
{
    "identifier": "user@example.com",
    "password": "pass123",
    "provider": "LOCAL"  // ← Sélectionne LocalStrategy
}

POST /api/auth/oauth/login
{
    "accessToken": "token",
    "provider": "GOOGLE"  // ← Sélectionne GoogleStrategy
}
```

## 🔄 Diagramme de Séquence

```
Client          Controller         Service           Strategy
  │                 │                 │                  │
  │─── POST /login ──►                │                  │
  │                 │                 │                  │
  │                 │─── authenticate()──►               │
  │                 │                 │                  │
  │                 │                 │─── selectStrategy()
  │                 │                 │                  │
  │                 │                 │─── authenticate()──►
  │                 │                 │                  │
  │                 │                 │◄─── AuthResponse ─┤
  │                 │                 │                  │
  │                 │◄─── AuthResponse─┤                 │
  │                 │                 │                  │
  │◄── 200 OK ──────┤                 │                  │
  │                 │                 │                  │
```

## 📊 Comparaison Code

### ❌ Sans Strategy Pattern (200 lignes)

```java
public class AuthService {
    public AuthResponse authenticate(LoginRequest request) {
        if (request.getProvider() == LOCAL) {
            // 50 lignes de logique locale
            User user = userRepo.findByUsername(request.getIdentifier());
            if (user == null) return error();
            if (!checkPassword(request.getPassword(), user.getPassword())) return error();
            // ... plus de code ...
            return success(user);
        } else if (request.getProvider() == GOOGLE) {
            // 50 lignes de logique Google
            if (!verifyGoogleToken(request.getToken())) return error();
            // ... plus de code ...
        } else if (request.getProvider() == GITHUB) {
            // 50 lignes de logique GitHub
            if (!verifyGithubToken(request.getToken())) return error();
            // ... plus de code ...
        }
        return error("Unsupported provider");
    }
}
```

### ✅ Avec Strategy Pattern (Propre et maintenable)

```java
@Service
public class AuthenticationService {
    private final List<AuthenticationStrategy> strategies;
    
    public AuthResponse authenticate(LoginRequest request) {
        AuthenticationStrategy strategy = selectStrategy(request.getProvider().name());
        if (strategy == null) return error("Unsupported provider");
        return strategy.authenticate(request);
    }
}

// Chaque stratégie dans son propre fichier
@Component
public class LocalAuthenticationStrategy implements AuthenticationStrategy {
    public AuthResponse authenticate(LoginRequest request) {
        // 50 lignes uniquement pour la logique locale
    }
}

@Component
public class GoogleAuthenticationStrategy implements AuthenticationStrategy {
    public AuthResponse authenticateOAuth(OAuthLoginRequest request) {
        // 50 lignes uniquement pour Google
    }
}
```

## 🎓 Concepts Clés

### 1. Dépendance par Injection (Spring)
Spring injecte automatiquement toutes les implémentations de `AuthenticationStrategy` :

```java
public AuthenticationService(List<AuthenticationStrategy> strategies) {
    this.strategies = strategies; // Spring injecte la liste complète !
}
```

### 2. Polymorphisme
Toutes les stratégies implémentent la même interface, donc on peut les traiter uniformément :

```java
AuthenticationStrategy strategy = selectStrategy(provider);
strategy.authenticate(request); // Peu importe quelle stratégie !
```

### 3. Composition over Inheritance
Au lieu d'hériter d'une classe de base, on compose avec des stratégies :
- Plus flexible
- Pas de couplage fort
- Facile de changer de stratégie

## 🚀 Cas d'Usage Réels

### 1. Paiements
```java
interface PaymentStrategy {
    PaymentResult pay(Order order);
}

class CreditCardStrategy implements PaymentStrategy { }
class PayPalStrategy implements PaymentStrategy { }
class CryptoStrategy implements PaymentStrategy { }
```

### 2. Compression
```java
interface CompressionStrategy {
    byte[] compress(File file);
}

class ZipCompressionStrategy implements CompressionStrategy { }
class GzipCompressionStrategy implements CompressionStrategy { }
class RarCompressionStrategy implements CompressionStrategy { }
```

### 3. Notification
```java
interface NotificationStrategy {
    void send(Message message);
}

class EmailNotificationStrategy implements NotificationStrategy { }
class SMSNotificationStrategy implements NotificationStrategy { }
class PushNotificationStrategy implements NotificationStrategy { }
```

## 📝 Bonnes Pratiques

1. **Une stratégie = Une responsabilité**
   - Ne mélangez pas plusieurs algorithmes dans une stratégie

2. **Interface claire et simple**
   - Méthodes bien définies
   - Pas trop de méthodes

3. **Factory ou Context pour la sélection**
   - Ne laissez pas le client choisir directement la stratégie

4. **Utiliser Spring pour l'injection**
   - `@Component` sur chaque stratégie
   - Injection automatique de la liste

5. **Tests unitaires pour chaque stratégie**
   - Testez indépendamment
   - Mockez les dépendances

## 🎯 Conclusion

Le **Strategy Pattern** est parfait pour notre service d'authentification car :

✅ **Extensibilité** : Ajouter un nouveau provider (Facebook, Twitter, etc.) est trivial  
✅ **Maintenabilité** : Chaque méthode d'auth est isolée  
✅ **Testabilité** : Tests unitaires simples et efficaces  
✅ **Lisibilité** : Code propre et organisé  
✅ **Flexibilité** : Facile de modifier ou remplacer une stratégie  

---

**Ressources Supplémentaires :**
- [Refactoring Guru - Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Design Patterns - Gang of Four](https://en.wikipedia.org/wiki/Design_Patterns)
- [Spring Design Patterns](https://www.baeldung.com/spring-framework-design-patterns)
