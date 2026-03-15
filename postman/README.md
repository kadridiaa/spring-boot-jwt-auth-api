# Postman - Auth Service

## Fichiers

- `auth-service.postman_collection.json`
- `auth-service.postman_environment.json`

## Import dans Postman

1. Ouvrir Postman.
2. Cliquer sur **Import**.
3. Importer les deux fichiers ci-dessus.
4. Sélectionner l'environnement **Auth Service - Local**.
5. Lancer la collection **Auth Service API (Current)**.

## Pré-requis

- `auth-service` doit répondre sur `http://localhost:8080`.
- RabbitMQ doit être démarré (le register publie des événements).

## Exécution automatique (CLI)

Depuis la racine du projet :

```bash
docker run --rm --network host \
  -v "$PWD/postman:/etc/newman" \
  postman/newman:alpine run /etc/newman/auth-service.postman_collection.json \
  -e /etc/newman/auth-service.postman_environment.json \
  --reporters cli,json \
  --reporter-json-export /etc/newman/newman-report.json
```

Le rapport JSON est généré dans : `postman/newman-report.json`.
