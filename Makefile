.PHONY: help build start stop restart logs clean test

# Variables
PROJECT_NAME = microservices-parent
AUTH_SERVICE = auth-service

help: ## Afficher l'aide
	@echo "Commandes disponibles:"
	@echo "  make build      - Build tous les services"
	@echo "  make start      - Démarrer tous les services"
	@echo "  make stop       - Arrêter tous les services"
	@echo "  make restart    - Redémarrer tous les services"
	@echo "  make logs       - Afficher les logs"
	@echo "  make clean      - Nettoyer le projet"
	@echo "  make test       - Lancer les tests"
	@echo "  make dev        - Démarrer auth-service en mode dev"

build: ## Build le projet avec Maven
	@echo "🏗️  Building project..."
	mvn clean install

start: ## Démarrer les services avec Docker Compose
	@echo "🚀 Starting services..."
	docker-compose up -d
	@echo "⏳ Waiting for services to start..."
	@sleep 10
	@echo "✅ Services started!"
	@echo "📡 Auth Service: http://localhost:8080/api/auth/health"

stop: ## Arrêter les services
	@echo "🛑 Stopping services..."
	docker-compose down

restart: stop start ## Redémarrer les services

logs: ## Afficher les logs des services
	docker-compose logs -f $(AUTH_SERVICE)

clean: ## Nettoyer le projet
	@echo "🧹 Cleaning project..."
	mvn clean
	docker-compose down -v
	@echo "✅ Cleanup complete!"

test: ## Lancer les tests
	@echo "🧪 Running tests..."
	mvn test

dev: ## Démarrer auth-service en mode développement
	@echo "🔧 Starting auth-service in dev mode..."
	cd $(AUTH_SERVICE) && mvn spring-boot:run

docker-build: ## Build les images Docker
	@echo "🐳 Building Docker images..."
	docker-compose build

docker-rebuild: ## Rebuild et restart les services
	@echo "🔄 Rebuilding and restarting services..."
	docker-compose up --build -d

status: ## Vérifier le status des services
	@echo "📊 Service status:"
	@docker-compose ps
	@echo ""
	@echo "🏥 Health checks:"
	@curl -s http://localhost:8080/api/auth/health | jq . || echo "Service not ready"

install: ## Installation complète
	@echo "📦 Complete installation..."
	make build
	make docker-build
	make start
	@echo "✅ Installation complete!"
