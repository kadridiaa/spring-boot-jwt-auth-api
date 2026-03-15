#!/bin/bash
set -e

echo "[1/13] Redémarrage complet de l'environnement..."
docker compose down -v
docker compose build auth-service discovery-service
docker compose up -d

echo "[2/13] Attente du démarrage des services (Gateway et Auth)..."
until curl -s http://localhost:8080/api/auth/health | grep -q "UP"; do
  sleep 2
done
until curl -s http://localhost:8082/actuator/health | grep -q "UP"; do
  sleep 2
done
sleep 5 # extra padding

GATEWAY="http://localhost:8082"

echo "[3/13] Création de l'Administrateur Initial..."
curl -s -X POST $GATEWAY/auth-service/api/auth/setup \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "email": "admin@sys.com", "password": "pass"}' | jq .

echo "[4/13] Inscription d'un utilisateur (user@sys.com)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "email": "user@sys.com", "password": "pass"}' | jq .

echo "[5/13] Login sans vérification (Doit échouer)..."
curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@sys.com", "password": "pass"}' | jq .

echo "[6/13] Récupération du lien Mailhog et Vérification..."
sleep 3
MAIL_BODY=$(curl -s http://localhost:8025/api/v2/search?kind=to\&query=user@sys.com | jq -r '.items[0].Content.Body' | tr -d '\r')
VERIFY_URL=$(echo "$MAIL_BODY" | grep -o 'http://[^\s]*' | grep 'verify' | head -n 1)
echo "Lien: $VERIFY_URL"
# remplacer 8080 par le gateway
V_GW=$(echo "$VERIFY_URL" | sed 's/8080/8082\/auth-service/')
curl -s "$V_GW" | jq .

echo "[7/13] Login avec succès..."
USER_TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@sys.com", "password": "pass"}' | jq -r .token)
echo "User Token obtenu."

echo "[8/13] Accès Service A (Doit être Refusé)..."
curl -s -w "\nHTTP: %{http_code}\n" -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $USER_TOKEN"

echo "[9/13] Tentative auto-attribution des droits (Doit échouer)..."
curl -s -w "\nHTTP: %{http_code}\n" -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=user@sys.com" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]'

echo "[10/13] Connexion Admin et attribution des droits..."
ADMIN_TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@sys.com", "password": "pass"}' | jq -r .token)

curl -s -X PUT "$GATEWAY/auth-service/api/auth/permissions?email=user@sys.com" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '["ACCESS_A", "ACCESS_B"]' | jq .

echo "[11/13] Re-connexion User pour actualiser le token..."
USER_TOKEN_NEW=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@sys.com", "password": "pass"}' | jq -r .token)

echo "[12/13] Accès Service A avec nouveaux droits (Doit réussir)..."
curl -s -X GET $GATEWAY/service-a/api/service-a/hello \
  -H "Authorization: Bearer $USER_TOKEN_NEW"
echo ""

echo "[13/13] Accès Service B avec nouveaux droits (Doit réussir)..."
curl -s -X GET $GATEWAY/service-b/api/service-b/hello \
  -H "Authorization: Bearer $USER_TOKEN_NEW"
echo ""

