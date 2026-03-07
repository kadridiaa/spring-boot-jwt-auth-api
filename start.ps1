# Script PowerShell pour démarrer l'architecture microservices

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  Démarrage Architecture Microservices  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Vérifier si Docker est installé
$dockerInstalled = Get-Command docker -ErrorAction SilentlyContinue
if (-not $dockerInstalled) {
    Write-Host "❌ Docker n'est pas installé. Veuillez installer Docker." -ForegroundColor Red
    exit 1
}

# Vérifier si Docker Compose est installé
$composeInstalled = Get-Command docker-compose -ErrorAction SilentlyContinue
if (-not $composeInstalled) {
    Write-Host "❌ Docker Compose n'est pas installé. Veuillez installer Docker Compose." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker et Docker Compose sont installés" -ForegroundColor Green
Write-Host ""

# Build et démarrage des services
Write-Host "🏗️  Build et démarrage des services..." -ForegroundColor Yellow
docker-compose up --build -d

Write-Host ""
Write-Host "⏳ Attente du démarrage des services (30 secondes)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "  Services démarrés avec succès! 🚀     " -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📡 Auth Service:" -ForegroundColor Cyan
Write-Host "   - API: http://localhost:8080/api/auth"
Write-Host "   - Health: http://localhost:8080/api/auth/health"
Write-Host "   - H2 Console: http://localhost:8080/h2-console"
Write-Host ""
Write-Host "📊 Vérification de l'état des services..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/health" -Method Get
    Write-Host "✅ Auth Service: UP" -ForegroundColor Green
    $response | ConvertTo-Json
} catch {
    Write-Host "⚠️  Service en cours de démarrage..." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "📝 Pour voir les logs:" -ForegroundColor Cyan
Write-Host "   docker-compose logs -f auth-service"
Write-Host ""
Write-Host "🛑 Pour arrêter les services:" -ForegroundColor Cyan
Write-Host "   docker-compose down"
Write-Host ""
