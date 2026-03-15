#!/bin/bash
export GATEWAY="http://localhost:8082"

# Inscription d'un nouvel utilisateur
curl -s -X POST $GATEWAY/auth-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser5", "email": "testuse5r@test.com", "password": "pass"}' > /dev/null

sleep 2

# Obtenir le lien de vérification
MAIL_BODY=$(curl -s http://localhost:8025/api/v2/messages | jq -r '.items[0].Content.Body' | tr -d '\r')
VERIFY_URL=$(echo "$MAIL_BODY" | grep -o 'http://localhost:8080/api/auth/verify[^ "]*')
echo "URL de vérification trouvée : $VERIFY_URL"

# Appeler l'URL (sans utiliser le gateway, en direct)
curl -s "$VERIFY_URL" | jq .

# Connexion pour obtenir le token
TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "testuse5r@test.com", "password": "pass"}' | jq -r .token)
echo "Token: $TOKEN"

# Accès au service A
echo "Tentative Service A avec TestUser5:"
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $TOKEN"

