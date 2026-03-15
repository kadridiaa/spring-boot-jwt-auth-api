#!/bin/bash
USER="testuser8"
EMAIL="test8@test.com"
export GATEWAY="http://localhost:8082"

curl -s -X POST $GATEWAY/auth-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\": \"$USER\", \"email\": \"$EMAIL\", \"password\": \"pass\"}" > /dev/null

sleep 2

# Search mailhog for the right email
MAIL_BODY=$(curl -s http://localhost:8025/api/v2/search?kind=to\&query=$EMAIL | jq -r '.items[0].Content.Body' | tr -d '\r')
VERIFY_URL=$(echo "$MAIL_BODY" | grep -o 'http://localhost:8080/api/auth/verify[^ "]*')
echo "URL pour $EMAIL: $VERIFY_URL"

# Appel verif
curl -s "$VERIFY_URL" | jq .

# Appel login
TOKEN=$(curl -s -X POST $GATEWAY/auth-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$EMAIL\", \"password\": \"pass\"}" | jq -r .token)

echo "Token pour $EMAIL : $TOKEN"
