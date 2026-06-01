# run-server.ps1 — Chạy server trên Windows (PowerShell).
#
#   Usage:
#     .\run-server.ps1                          # default port 5050
#     .\run-server.ps1 -DbPassword "xxx"         # với DB password
#     .\run-server.ps1 -ServerPort 6060         # custom port
#     .\run-server.ps1 -SkipBuild               # skip build
#
#   Hoặc dùng deploy.sh cho toàn bộ hệ thống (server + client):
#     bash deploy.sh

param(
    [Parameter(Mandatory = $false)]
    [string]$DbPassword,

    [Parameter(Mandatory = $false)]
    [int]$ServerPort = 5050,

    [Parameter(Mandatory = $false)]
    [string]$BindHost = "0.0.0.0",

    [Parameter(Mandatory = $false)]
    [switch]$SkipBuild,

    [Parameter(Mandatory = $false)]
    [switch]$SkipTests,

    [Parameter(Mandatory = $false)]
    [switch]$Tls,

    [Parameter(Mandatory = $false)]
    [string]$KeyStore,

    [Parameter(Mandatory = $false)]
    [string]$KeyStorePassword
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
if ($root -eq "") { $root = "." }
$repoRoot = Split-Path -Parent $root
Set-Location $repoRoot
. (Join-Path $repoRoot "scripts\use-jdk21.ps1")

# Load DB_PASSWORD from environment if not provided
if (-not $DbPassword) {
    $DbPassword = $env:DB_PASSWORD
}

# Set environment variables
$env:APP_SERVER_PORT = "$ServerPort"
$env:APP_SERVER_BIND_HOST = "$BindHost"
$env:SKIP_TESTS = if ($SkipTests) { "true" } else { "false" }

# Pass the DB password to the Java process without writing it to the source tree.
if ($DbPassword) {
    $env:DB_PASSWORD = $DbPassword
} else {
    Write-Warning "DB_PASSWORD not set. Server may fail if database.properties has wrong password."
}

# Build
if (-not $SkipBuild) {
    Write-Host "[INFO] Building project..." -ForegroundColor Cyan
    mvn -DskipTests package -q
    if ($LASTEXITCODE -ne 0) { Write-Host "[ERROR] Build failed." -ForegroundColor Red; exit $LASTEXITCODE }
    Write-Host "[OK] Build complete." -ForegroundColor Green
}

# Validate JAR
$jarPath = "server\target\server-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $jarPath)) {
    Write-Host "[ERROR] JAR not found: $jarPath" -ForegroundColor Red
    exit 1
}
$jarSize = (Get-Item $jarPath).Length / 1MB
if ($jarSize -lt 1) {
    Write-Warning "JAR suspiciously small ($([math]::Round($jarSize, 2)) MB)."
}
Write-Host "[OK] JAR validated: $jarPath ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green

# Build classpath
$cp = $jarPath
$depDir = "server\target\dependency"
if (Test-Path $depDir) {
    foreach ($jar in Get-ChildItem "$depDir\*.jar") {
        $cp = "$cp;$($jar.FullName)"
    }
}

# Setup logs directory
$logDir = "logs"
$logFile = "$logDir\server.log"
$stderrLog = "$logDir\server.err.log"
$pidFile = "$logDir\server.pid"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# Stop existing server
Write-Host "[INFO] Stopping any running server..." -ForegroundColor Cyan
if (Test-Path $pidFile) {
    $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
        Write-Host "[INFO] Stopping PID $oldPid gracefully..." -ForegroundColor Cyan
        Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
# Kill any stray processes
Get-Process | Where-Object { $_.ProcessName -like "*java*" -and $_.CommandLine -like "*AuctionServerMain*" } -ErrorAction SilentlyContinue | ForEach-Object {
    Write-Host "[WARN] Killing stray process: $($_.Id)" -ForegroundColor Yellow
    Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
}

# Backup log
if (Test-Path $logFile) {
    $logSize = (Get-Item $logFile).Length / 1MB
    Write-Host "[INFO] Backing up old log ($([math]::Round($logSize, 2)) MB)..." -ForegroundColor Cyan
    $oldLog = "$logFile.bak"
    if (Test-Path $oldLog) { Remove-Item $oldLog -Force }
    Move-Item $logFile $oldLog -Force
}

# Start server
Write-Host "[INFO] Starting server on ${BindHost}:${ServerPort}..." -ForegroundColor Cyan
$javaArgs = @(
    "-Xmx512m",
    "-Xms128m",
    "-Djava.awt.headless=true",
    "-Dapp.server.port=$ServerPort",
    "-Dapp.server.bind.host=$BindHost",
    "-Dapp.server.tls.enabled=$($Tls.IsPresent.ToString().ToLowerInvariant())"
)
if ($KeyStore) {
    $javaArgs += "-Djavax.net.ssl.keyStore=$KeyStore"
}
if ($KeyStorePassword) {
    $javaArgs += "-Djavax.net.ssl.keyStorePassword=$KeyStorePassword"
}
$process = Start-Process -FilePath "java" -ArgumentList ($javaArgs + @("-cp", $cp, "userauth.server.AuctionServerMain")) `
    -WindowStyle Hidden -PassThru -RedirectStandardOutput $logFile -RedirectStandardError $stderrLog
$process.Id | Set-Content $pidFile
Write-Host "[OK] Server started (PID $($process.Id))" -ForegroundColor Green

# Wait for startup
Write-Host "[INFO] Waiting for startup (max 60 seconds)..." -ForegroundColor Cyan
$startupOk = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 1
    if ($process.HasExited) {
        Write-Host "[ERROR] Server died during startup." -ForegroundColor Red
        if (Test-Path $stderrLog) { Get-Content $stderrLog | Select-Object -Last 10 }
        if (Test-Path $logFile) { Get-Content $logFile | Select-Object -Last 10 }
        exit 1
    }
    $portInUse = Get-NetTCPConnection -LocalPort $ServerPort -ErrorAction SilentlyContinue
    if ($portInUse) {
        $startupOk = $true
        Write-Host "[OK] Port $ServerPort is listening." -ForegroundColor Green
        break
    }
    if (Test-Path $logFile) {
        $listenLine = Select-String -Path $logFile -Pattern "Listening|Server started" -ErrorAction SilentlyContinue
        if ($listenLine) {
            $startupOk = $true
            Write-Host "[OK] Server startup confirmed in log." -ForegroundColor Green
            break
        }
    }
    if ($i % 10 -eq 0) {
        Write-Host "[INFO]   Still waiting... ($i seconds)" -ForegroundColor Cyan
    }
}

if (-not $startupOk -and $process.HasExited) {
    Write-Host "[ERROR] Server died. Check logs." -ForegroundColor Red
    if (Test-Path $stderrLog) { Get-Content $stderrLog | Select-Object -Last 10 }
    exit 1
}

# Verify DB
if (Test-Path $logFile) {
    $dbLine = Select-String -Path $logFile -Pattern "\[Database\] Connected|Connected successfully|Database initialized" -ErrorAction SilentlyContinue
    if ($dbLine) {
        Write-Host "[OK] Database: Connected" -ForegroundColor Green
    }
    $dbError = Select-String -Path $logFile -Pattern "Could not connect|Connection refused|Communications link" -ErrorAction SilentlyContinue
    if ($dbError) {
        Write-Warning "Database connection issues detected. Check $logFile."
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Server deployed successfully!" -ForegroundColor Green
Write-Host "  PID:     $($process.Id)" -ForegroundColor Green
Write-Host "  Port:    ${BindHost}:${ServerPort}" -ForegroundColor Green
Write-Host "  Log:     $logFile" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "To follow logs:   Get-Content $logFile -Wait -Tail 30"
Write-Host "To check stderr: Get-Content $stderrLog -Wait -Tail 30"
Write-Host "To stop server:  Stop-Process (Get-Content $pidFile)"
Write-Host ""
