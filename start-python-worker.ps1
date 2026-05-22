# ============================================================
# Script khởi động Python AI Media Worker độc lập
# Yêu cầu: Python 3.11+, FFmpeg (tùy chọn nhưng khuyến nghị)
# ============================================================

Write-Host "=== PYTHON AI MEDIA WORKER STARTUP ===" -ForegroundColor Cyan

$WorkerPath = "$PWD\python-services\ai-media-worker"
if (-not (Test-Path $WorkerPath)) {
    Write-Host "Error: Cannot find python-services\ai-media-worker directory." -ForegroundColor Red
    exit 1
}

cd $WorkerPath

# Bước 1: Tạo môi trường ảo (venv) nếu chưa có
if (-not (Test-Path "venv")) {
    Write-Host "`n[1/3] Creating Python Virtual Environment (venv)..." -ForegroundColor Yellow
    python -m venv venv
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to create venv. Please ensure Python is installed and added to PATH." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "`n[1/3] Python Virtual Environment already exists." -ForegroundColor Green
}

# Bước 2: Cài đặt thư viện
Write-Host "`n[2/3] Activating venv and installing dependencies..." -ForegroundColor Yellow
& ".\venv\Scripts\python.exe" -m pip install --upgrade pip | Out-Null
& ".\venv\Scripts\pip.exe" install -r requirements.txt
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to install dependencies." -ForegroundColor Red
    exit 1
}

# Bước 3: Khởi động FastAPI & gRPC Server
Write-Host "`n[3/3] Starting Uvicorn Server (FastAPI + gRPC)..." -ForegroundColor Yellow
Write-Host "HTTP Port: 8765 | gRPC Port: 50051" -ForegroundColor Cyan
Write-Host "Swagger UI: http://localhost:8765/docs`n" -ForegroundColor Green

# Load biến môi trường từ thư mục gốc nếu có (dành cho AI_WORKER_HTTP_PORT / gRPC config)
if (Test-Path "..\..\.env") {
    foreach ($line in Get-Content "..\..\.env") {
        if ($line -match '^([^#\s][^=]*)=(.*)$') {
            $env:$($matches[1].Trim()) = $matches[2].Trim().Trim('"').Trim("'")
        }
    }
}

# Chạy server
& ".\venv\Scripts\uvicorn.exe" main:app --host 0.0.0.0 --port 8765 --reload
