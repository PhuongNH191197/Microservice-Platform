# ============================================================
# Script khởi động toàn bộ Microservice Platform trên LOCALHOST
# Yêu cầu: Maven, Java 21, Python 3.11, Docker (cho DB/Middleware)
# ============================================================

Write-Host "=== MICROSERVICE PLATFORM - LOCALHOST STARTUP ===" -ForegroundColor Cyan

# Bước 1: Khởi động hạ tầng (Database & Middleware) qua Docker
Write-Host "`n[1/5] Starting infrastructure (Postgres, Redis, RabbitMQ, MinIO)..." -ForegroundColor Yellow
docker-compose up -d postgres redis rabbitmq minio
Start-Sleep -Seconds 10

# Bước 2: Build toàn bộ project
Write-Host "`n[2/5] Building all Maven modules..." -ForegroundColor Yellow
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. Exiting." -ForegroundColor Red
    exit 1
}

# Bước 3: Khởi động Eureka Server (BẮT BUỘC ĐẦU TIÊN)
Write-Host "`n[3/5] Starting Eureka Server (port 8761)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run -pl infrastructure/eureka-server"
Start-Sleep -Seconds 20

# Bước 4: Khởi động Config Server (THỨ HAI)
Write-Host "`n[4/5] Starting Config Server (port 8888)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run -pl infrastructure/config-server"
Start-Sleep -Seconds 15

# Bước 5: Khởi động các Infra Services (song song)
Write-Host "`n[5/5] Starting Infra Services..." -ForegroundColor Yellow
$infraServices = @(
    "infra-services/auth-service",
    "infra-services/credit-wallet-service",
    "infra-services/file-service",
    "infra-services/notification-service",
    "infra-services/audit-log-service",
    "infra-services/payment-gateway-service"
)

foreach ($service in $infraServices) {
    $serviceName = Split-Path $service -Leaf
    Write-Host "  -> Starting $serviceName..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run -pl $service"
    Start-Sleep -Seconds 3
}

Start-Sleep -Seconds 20

# Bước 6: Khởi động các Business Services (song song)
Write-Host "`nStarting Business Services..." -ForegroundColor Yellow
$businessServices = @(
    "business-services/crbt-campaign-service",
    "business-services/crbt-community-library",
    "business-services/audio-generation-service",
    "business-services/crbt-credit-transaction-service",
    "business-services/crbt-core-adapter"
)

foreach ($service in $businessServices) {
    $serviceName = Split-Path $service -Leaf
    Write-Host "  -> Starting $serviceName..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run -pl $service"
    Start-Sleep -Seconds 3
}

Start-Sleep -Seconds 20

# Bước 7: Khởi động API Gateway (SAU CÙNG)
Write-Host "`nStarting API Gateway (port 8080)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; mvn spring-boot:run -pl infrastructure/api-gateway"

# Bước 8: Khởi động Python AI Worker (nếu cần)
Write-Host "`nStarting Python AI Worker (port 8765)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD/python-services/ai-media-worker'; uvicorn main:app --host 0.0.0.0 --port 8765 --reload"

Write-Host "`n=== ALL SERVICES STARTED ===" -ForegroundColor Green
Write-Host "Eureka Dashboard: http://localhost:8761 (eureka / eureka-secret)" -ForegroundColor Cyan
Write-Host "API Gateway: http://localhost:8080" -ForegroundColor Cyan
Write-Host "RabbitMQ Management: http://localhost:15672 (guest / guest)" -ForegroundColor Cyan
Write-Host "MinIO Console: http://localhost:9001 (minioadmin / minioadmin)" -ForegroundColor Cyan
