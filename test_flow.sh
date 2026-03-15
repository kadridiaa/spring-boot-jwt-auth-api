#!/bin/bash
echo "=== Démarrage du test d'intégration PBAC ==="
sleep 2

GATEWAY="http://localhost:8082"

echo "1. Création du compte Administrateur Initial..."
curl -s -X POST $GATEWAY/auth-service/api/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "email": "admin4@test.com", "password": "adminpass"}' | jq .
echo -e "\n"

echo "2. Inscription d'un utilisateur normal (bob)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "bob", "email": "bob4@test.com", "password": "bobpass"}' | jq .
echo -e "\n"

echo "3. Tentative de login de bob (devrait échouer car non vérifié)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "bob4@test.com", "password": "bobpass"}' | jq .
echo -e "\n"

echo "4. Récupération du lien de vérification depuis MailHog..."
sleep 2
MAIL_BODY=$(curl -s http://localhost:8025/api/v2/messages | jq -r '.items[0].Content.Body')
VERIFY_URL=$(echo "$MAIL_BODY" | grep -o 'http://localhost:8080/api/auth/verify[^ "]*')
echo "Lien trouvé : $VERIFY_URL"
echo -e "\n"

echo "5. Vérification du compte de bob..."
# On remplace 8080 par 8082/auth-service pour passer par la gateway (bien que 8080 marche aussi)
VERIFY_URL_GATEWAY=$(echo $VERIFY_URL | sed 's/8080/8082\/auth-service/')
curl -s "$VERIFY_URL_GATEWAY" | jq .
echo -e "\n"

echo "6. Connexion de bob (succès) et récupération du token..."
BOB_RESP=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "bob4@test.com", "password": "bobpass"}')
echo $BOB_RESP | jq .
BOB_TOKEN=$(echo $BOB_RESP | jq -r .token)
echo -e "\n"

echo "7. Tentative d'accès de bob au Service A (devrait échouer : 403)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $BOB_TOKEN"
echo -e "\n"

echo "8. Tentative d'auto-attribution des permissions par bob (devrait échouer)..."
curl -s -w "\nHTTP Status: %{http_code}\n" -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=bob4@test.com" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $BOB_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]'
echo -e "\n"

echo "9. Connexion de l'admin pour récupération de son token..."
ADMIN_RESP=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin4@test.com", "password": "adminpass"}')
echo $ADMIN_RESP | jq .
ADMIN_TOKEN=$(echo $ADMIN_RESP | jq -r .token)
echo -e "\n"

echo "10. L'admin attribue les permissions à bob..."
curl -s -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=bob4@test.com" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]' | jq .
echo -e "\n"

echo "11. Bob se reconnecte pour obtenir un token avec les nouvelles permissions..."
BOB_RESP2=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "bob4@test.com", "password": "bobpass"}')
BOB_TOKEN2=$(echo $BOB_RESP2 | jq -r .token)
echo "Nouveau token généré pour bob."
echo -e "\n"

echo "12. Bob accède au Service A avec succès..."
curl -s -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $BOB_TOKEN2"
echo -e "\n\n"

echo "13. Bob accède au Service B avec succès..."
curl -s -X GET $GATEWAY/service-b/api/service-b/hello \
  -H "Authorization: Bearer $BOB_TOKEN2"
echo -e "\n"

