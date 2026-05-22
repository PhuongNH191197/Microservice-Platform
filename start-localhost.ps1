# ============================================================
# Script khởi động Microservice Platform - HỖ TRỢ LOAD .ENV
# ============================================================

Write-Host "=== MICROSERVICE PLATFORM - LOCALHOST STARTUP ===" -ForegroundColor Cyan

# --- BƯỚC 0: LOAD BIẾN MÔI TRƯỜNG TỪ FILE .ENV ---
if (Test-Path ".env") {
    Write-Host "`n[0/5] Loading environment variables from .env..." -ForegroundColor Yellow
    foreach ($line in Get-Content .env) {
        if ($line -match '^([^#\s][^=]*)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($key, $value, [System.EnvironmentVariableTarget]::Process)
            # Write-Host "  Loaded: $key" -ForegroundColor Gray
        }
    }
    Write-Host "  .env loaded successfully!" -ForegroundColor Green
} else {
    Write-Host "  Warning: .env file not found!" -ForegroundColor Red
}

# Bước 1: Khởi động hạ tầng Docker
Write-Host "`n[1/5] Starting infrastructure (Postgres, Redis, RabbitMQ, MinIO)..." -ForegroundColor Yellow
docker-compose up -d postgres redis rabbitmq minio
Start-Sleep -Seconds 10

# Bước 2: Build project (Dùng mvn nếu có, hoặc báo lỗi nếu không)
Write-Host "`n[2/5] Building all Maven modules..." -ForegroundColor Yellow
mvn clean install -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed. If 'mvn' is not found, please install it or build in IntelliJ first." -ForegroundColor Red
    exit 1
}

# Bước 3: Khởi động Eureka
Write-Host "`n[3/5] Starting Eureka Server (port 8761)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; foreach (`$line in Get-Content .env) { if (`$line -match '^([^#\s][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim().Trim('`\"').Trim('\''), [System.EnvironmentVariableTarget]::Process) } }; mvn spring-boot:run -pl infrastructure/eureka-server"
Start-Sleep -Seconds 20

# Bước 4: Khởi động Config Server
Write-Host "`n[4/5] Starting Config Server (port 8888)..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; foreach (`$line in Get-Content .env) { if (`$line -match '^([^#\s][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim().Trim('`\"').Trim('\''), [System.EnvironmentVariableTarget]::Process) } }; mvn spring-boot:run -pl infrastructure/config-server"
Start-Sleep -Seconds 15

# Bước 5: Các service còn lại (Logic tương tự, inject .env vào từng window)
$services = @(
    "infra-services/auth-service",
    "infra-services/credit-wallet-service",
    "infra-services/file-service",
    "infra-services/notification-service",
    "infra-services/audit-log-service",
    "infra-services/payment-gateway-service",
    "business-services/crbt-campaign-service",
    "business-services/crbt-community-library",
    "business-services/audio-generation-service",
    "business-services/crbt-credit-transaction-service",
    "business-services/crbt-core-adapter"
)

Write-Host "`n[5/5] Starting Services..." -ForegroundColor Yellow
foreach ($s in $services) {
    Write-Host "  -> Launching $s..." -ForegroundColor Gray
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; foreach (`$line in Get-Content .env) { if (`$line -match '^([^#\s][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim().Trim('`\"').Trim('\''), [System.EnvironmentVariableTarget]::Process) } }; mvn spring-boot:run -pl $s"
    Start-Sleep -Seconds 3
}

# API Gateway sau cùng
Write-Host "`nFinal: Starting API Gateway..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$PWD'; foreach (`$line in Get-Content .env) { if (`$line -match '^([^#\s][^=]*)=(.*)$') { [System.Environment]::SetEnvironmentVariable(`$matches[1].Trim(), `$matches[2].Trim().Trim('`\"').Trim('\''), [System.EnvironmentVariableTarget]::Process) } }; mvn spring-boot:run -pl infrastructure/api-gateway"

Write-Host "`n=== ALL SERVICES INJECTED WITH .ENV AND STARTED ===" -ForegroundColor Green
