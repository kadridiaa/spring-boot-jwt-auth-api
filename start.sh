#!/bin/bash

# Script pour démarrer l'architecture microservices

echo "========================================="
echo "  Démarrage Architecture Microservices  "
echo "========================================="
echo ""

# Vérifier si Docker est installé
if ! command -v docker &> /dev/null; then
    echo "❌ Docker n'est pas installé. Veuillez installer Docker."
    exit 1
fi

# Vérifier si Docker Compose est installé
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose n'est pas installé. Veuillez installer Docker Compose."
    exit 1
fi

echo "✅ Docker et Docker Compose sont installés"
echo ""

# Build et démarrage des services
echo "🏗️  Build et démarrage des services..."
docker-compose up --build -d

echo ""
echo "⏳ Attente du démarrage des services (30 secondes)..."
sleep 30

echo ""
echo "========================================="
echo "  Services démarrés avec succès! 🚀     "
echo "========================================="
echo ""
echo "📡 Auth Service:"
echo "   - API: http://localhost:8080/api/auth"
echo "   - Health: http://localhost:8080/api/auth/health"
echo "   - H2 Console: http://localhost:8080/h2-console"
echo ""
echo "📊 Vérification de l'état des services..."
curl -s http://localhost:8080/api/auth/health | json_pp || echo "Service en cours de démarrage..."
echo ""
echo "📝 Pour voir les logs:"
echo "   docker-compose logs -f auth-service"
echo ""
echo "🛑 Pour arrêter les services:"
echo "   docker-compose down"
echo ""
