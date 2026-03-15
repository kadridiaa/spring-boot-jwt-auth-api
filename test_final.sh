#!/bin/bash
set -e

# Test ID pour éviter les conflits
TEST_ID=$RANDOM
USER_EMAIL="user${TEST_ID}@sys.com"
USER_PASS="pass123"

# Admin par defaut cree par DataInitializer.java (vu dans le code source)
ADMIN_EMAIL="admin@example.com"
ADMIN_PASS="admin123"

GATEWAY="http://localhost:8082"

echo "=== Démarrage du test d'intégration PBAC et Microservices ==="
echo ""

echo "1. Inscription d'un utilisateur ($USER_EMAIL)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"user${TEST_ID}\", \"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}" | jq .

echo ""
echo "2. Login sans vérification (Doit échouer)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}" | jq .

echo ""
echo "3. Attente réception de l'email via RabbitMQ -> Notification -> Mailhog..."
sleep 6

MAIL_BODY=$(curl -s "http://localhost:8025/api/v2/search?kind=to&query=$USER_EMAIL" | jq -r '.items[0].Content.Body' | tr -d '\r')
VERIFY_URL=$(echo "$MAIL_BODY" | grep -o 'http://[^\s]*' | grep 'verify' | head -n 1)

echo "Lien de vérification trouvé: $VERIFY_URL"
echo ""

echo "4. Vérification via l'email..."
# On passe par le gateway pour valider le routage
V_GW=$(echo "$VERIFY_URL" | sed 's/8080/8082\/auth-service/')
curl -s "$V_GW" | jq .

echo ""
echo "5. Login après vérification (Succès)..."
USER_TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}" | jq -r .token)
echo "Token Utilisateur généré avec succès !"

echo ""
echo "6. Accès au Service A en tant qu'utilisateur (Doit être Refusé : 403)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $USER_TOKEN"

echo ""
echo "7. L'utilisateur essaye de s'ajouter des droits (Doit échouer : 403)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=$USER_EMAIL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]'

echo ""
echo ""
echo "8. Connexion de l'Administrateur..."
ADMIN_TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASS\"}" | jq -r .token)
echo "Token Admin généré avec succès !"

echo ""
echo "9. L'Administrateur accorde les rôles ACCESS_A et ACCESS_B à $USER_EMAIL..."
curl -s -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=$USER_EMAIL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]' | jq .

echo ""
echo "10. Re-connexion de l'utilisateur pour actualiser le token JWT..."
USER_TOKEN_NEW=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}" | jq -r .token)

echo ""
echo "11. Accès au Service A avec les Nouveaux Droits (Doit réussir)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $USER_TOKEN_NEW"

echo ""
echo ""
echo "12. Accès au Service B avec les Nouveaux Droits (Doit réussir)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $GATEWAY/service-b/api/service-b/hello \
  -H "Authorization: Bearer $USER_TOKEN_NEW"

echo ""
echo "=== Fin du test ==="
