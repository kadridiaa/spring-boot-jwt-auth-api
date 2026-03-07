# 🚀 Guide de Démarrage Rapide

## Prérequis

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (optionnel)

## Option 1 : Démarrage avec Maven (Développement)

### Étape 1 : Build le projet parent
```bash
cd spring-boot-jwt-auth-api
mvn clean install
```

### Étape 2 : Démarrer le service d'authentification
```bash
cd auth-service
mvn spring-boot:run
```

Le service sera disponible sur `http://localhost:8080`

### Étape 3 : Accéder à la console H2
Ouvrez votre navigateur : `http://localhost:8080/h2-console`

**Paramètres de connexion :**
- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: _(laisser vide)_

## Option 2 : Démarrage avec Docker Compose (Production-like)

### Windows PowerShell
```powershell
.\start.ps1
```

### Linux/Mac
```bash
chmod +x start.sh
./start.sh
```

### Ou manuellement
```bash
docker-compose up --build
```

Le service sera disponible sur `http://localhost:8080`

## Option 3 : Utiliser Makefile

```bash
# Build le projet
make build

# Démarrer tous les services
make start

# Voir les logs
make logs

# Arrêter les services
make stop

# Nettoyer
make clean
```

## 🧪 Tester l'Application

### Test 1 : Health Check
```bash
curl http://localhost:8080/api/auth/health
```

### Test 2 : Voir les providers supportés
```bash
curl http://localhost:8080/api/auth/providers
```

### Test 3 : Inscription d'un utilisateur
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "provider": "LOCAL"
  }'
```

### Test 4 : Connexion
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "testuser",
    "password": "password123",
    "provider": "LOCAL"
  }'
```

### Test 5 : OAuth Google (Simulation)
```bash
curl -X POST http://localhost:8080/api/auth/oauth/login \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken": "google_test_token",
    "provider": "GOOGLE",
    "email": "user@gmail.com",
    "username": "googleuser",
    "providerId": "google_123456"
  }'
```

## 📝 Utiliser VSCode REST Client

Le projet inclut un fichier `auth-service/API_EXAMPLES.http` avec tous les exemples de requêtes.

1. Installez l'extension "REST Client" dans VSCode
2. Ouvrez le fichier `API_EXAMPLES.http`
3. Cliquez sur "Send Request" au-dessus de chaque requête

## 🐛 Dépannage

### Le service ne démarre pas
```bash
# Vérifier les logs
docker-compose logs -f auth-service

# Ou avec Maven
cd auth-service
mvn spring-boot:run -X
```

### Port 8080 déjà utilisé
Modifiez le port dans `auth-service/src/main/resources/application.properties` :
```properties
server.port=8081
```

### Problème avec Docker
```bash
# Nettoyer les containers
docker-compose down -v

# Rebuild
docker-compose up --build
```

## 📊 Surveiller l'Application

### Actuator Endpoints
- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

### H2 Console (Dev uniquement)
`http://localhost:8080/h2-console`

## 📚 Prochaines Étapes

1. Consultez [README_MICROSERVICES.md](README_MICROSERVICES.md) pour la documentation complète
2. Consultez [auth-service/README.md](auth-service/README.md) pour les détails du service
3. Testez les différentes stratégies d'authentification
4. Explorez le code pour comprendre le Strategy Pattern

## 🎯 Scénarios de Test Recommandés

### Scénario 1 : Flux Local Complet
1. Inscription d'un nouvel utilisateur
2. Connexion avec username
3. Connexion avec email
4. Test avec mauvais mot de passe

### Scénario 2 : OAuth
1. Connexion OAuth Google
2. Connexion OAuth GitHub
3. Vérifier la création automatique de compte

### Scénario 3 : Validation
1. Tester username trop court
2. Tester email invalide
3. Tester mot de passe trop court
4. Tester username en double

## 🔗 Ressources Utiles

- [Guide Complet](./GUIDE_COMPLET_PROJET.md)
- [Explications Détaillées](./EXPLICATIONS_DETAILLEES.md)
- [Schémas Visuels](./SCHEMAS_VISUELS.md)
- [Documentation Spring Boot](https://spring.io/projects/spring-boot)

---

**Besoin d'aide ?** Consultez la documentation complète ou les logs du service.
