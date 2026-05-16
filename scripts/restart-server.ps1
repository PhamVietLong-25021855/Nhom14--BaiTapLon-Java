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
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

# ── Resolve project root ────────────────────────────────────────────────────────
$ProjectRoot = Split-Path -Parent $PSScriptRoot

# ── Defaults ───────────────────────────────────────────────────────────────────
if (-not $DbPassword) {
    $DbPassword = $env:DB_PASSWORD
}

if ($DbPassword) {
    $env:DB_PASSWORD = $DbPassword
} else {
    Write-Warning "DB_PASSWORD chua duoc truyen. Hay truyen -DbPassword hoac set bien moi truong DB_PASSWORD."
}

$env:APP_SERVER_PORT = "$ServerPort"
$env:APP_SERVER_BIND_HOST = "$BindHost"

$LogFile = "$ProjectRoot\logs\server.log"
$OldLogFile = "$ProjectRoot\logs\server.log.bak"
$StderrLog = "$ProjectRoot\logs\server.err.log"
$PidFile = "$ProjectRoot\logs\server.pid"
$JarFile = "$ProjectRoot\server\target\auction-server.jar"

function Write-Info($msg) { Write-Host "[INFO]   $msg" -ForegroundColor Cyan }
function Write-Success($msg) { Write-Host "[OK]     $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "[WARN]   $msg" -ForegroundColor Yellow }
function Write-Err($msg) {
    Write-Host "[ERROR]  $msg" -ForegroundColor Red
    exit 1
}

# ── Check JAR exists ────────────────────────────────────────────────────────────
if (-not (Test-Path $JarFile)) {
    Write-Err "JAR not found: $JarFile. Run deploy script first, or remove -SkipBuild."
}

# ── DB password injection ───────────────────────────────────────────────────────
$DbProps = "$ProjectRoot\server\src\main\resources\userauth\database.properties"
if (Test-Path $DbProps) {
    if ($DbPassword) {
        $content = Get-Content $DbProps -Raw
        $updated = $content -replace '(?m)^db\.password=.*', "db.password=$DbPassword"
        Set-Content -Path $DbProps -Value $updated -NoNewline
        Write-Info "DB password injected into database.properties"
    }
} else {
    Write-Warn "database.properties not found: $DbProps"
}

# ── Stop old server ─────────────────────────────────────────────────────────────
Write-Info "Stopping existing server..."

if (Test-Path $PidFile) {
    $oldPid = Get-Content $PidFile -ErrorAction SilentlyContinue
    if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
        Write-Info "Stopping PID $oldPid gracefully..."
        Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        if (Get-Process -Id $oldPid -ErrorAction SilentlyContinue) {
            Write-Warn "Force killing PID $oldPid..."
            Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue
        }
        Write-Success "Server stopped."
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# Kill by process name
$javaProcs = Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*AuctionServerMain*" -or $_.CommandLine -like "*auction-server*"
}
foreach ($proc in $javaProcs) {
    Write-Warn "Killing stray Java process PID $($proc.Id)..."
    Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
}
Start-Sleep -Seconds 1

# Check port
$portInUse = Get-NetTCPConnection -LocalPort $ServerPort -State Listen -ErrorAction SilentlyContinue
if ($portInUse) {
    Write-Warn "Port $ServerPort still in use. Attempting to free..."
    $portPid = $portInUse[0].OwningProcess
    Stop-Process -Id $portPid -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
}

# ── Backup log ──────────────────────────────────────────────────────────────────
if (Test-Path $LogFile) {
    $logSize = (Get-Item $LogFile).Length / 1MB
    Write-Info "Backing up old log ($([math]::Round($logSize, 1)) MB)..."
    if (Test-Path $OldLogFile) { Remove-Item $OldLogFile -Force }
    Move-Item $LogFile $OldLogFile -Force
}

# ── Validate JAR ────────────────────────────────────────────────────────────────
$jarSize = (Get-Item $JarFile).Length
if ($jarSize -eq 0) {
    Write-Err "JAR is empty."
}
$jarMb = [math]::Round($jarSize / 1MB, 2)
if ($jarSize -lt 1MB) {
    Write-Warn "JAR suspiciously small (${jarMb} MB)."
}
Write-Success "JAR validated: $JarFile (${jarMb} MB)"

# ── Build (if not skipped) ─────────────────────────────────────────────────────
if (-not $SkipBuild) {
    Write-Info "Building project (tests: $(if ($SkipTests) { 'skipped' } else { 'enabled' }))..."

    Push-Location $ProjectRoot
    $buildCmd = "mvn package"
    if ($SkipTests) { $buildCmd += " -DskipTests" }

    Invoke-Expression $buildCmd
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        Write-Err "Maven build failed (exit: $LASTEXITCODE)."
    }

    Invoke-Expression "mvn dependency:copy-dependencies -DincludeScope=runtime"
    if ($LASTEXITCODE -ne 0) {
        Pop-Location
        Write-Err "Maven dependency copy failed."
    }
    Pop-Location
    Write-Success "Build complete."
}

# ── Start server ───────────────────────────────────────────────────────────────
Write-Info "Starting server on ${BindHost}:${ServerPort}..."
$javaCmd = @(
    "java",
    "-Xmx512m", "-Xms128m",
    "-Djava.awt.headless=true",
    "-Dapp.server.port=$ServerPort",
    "-Dapp.server.bind.host=$BindHost",
    "-cp",
    "target\auction-server.jar",
    "userauth.server.AuctionServerMain"
)

$javaProcess = Start-Process -FilePath $javaCmd[0] -ArgumentList $javaCmd[1..($javaCmd.Length-1)] -WorkingDirectory $ProjectRoot -PassThru -NoNewWindow

$javaPid = $javaProcess.Id
$null = New-Item -ItemType Directory -Force "$ProjectRoot\logs" | Out-Null
$null = Set-Content -Path "$ProjectRoot\logs\server.pid" -Value $javaPid
Write-Success "Server started (PID $javaPid)"

# ── Wait for startup ───────────────────────────────────────────────────────────
Write-Info "Waiting for startup (max 45 seconds)..."

$startupOk = $false
for ($i = 1; $i -le 45; $i++) {
    Start-Sleep -Milliseconds 1000

    if ($javaProcess.HasExited) {
        $stderrContent = ""
        if (Test-Path $StderrLog) { $stderrContent = Get-Content $StderrLog -Raw }
        [string]::IsNullOrWhiteSpace($stderrContent) -and (Test-Path $LogFile) -and ($stderrContent = Get-Content $LogFile -Tail 30 -ErrorAction SilentlyContinue)
        Write-Err "Server died (exit: $($javaProcess.ExitCode)).`n$stderrContent"
    }

    $listening = Get-NetTCPConnection -LocalPort $ServerPort -State Listen -ErrorAction SilentlyContinue
    if ($listening) {
        $startupOk = $true
        Write-Success "Port $ServerPort is listening."
        break
    }

    if (Test-Path $LogFile) {
        $logContent = Get-Content $LogFile -Raw -ErrorAction SilentlyContinue
        if ($logContent -match "\[AuctionServer\] Listening on") {
            $startupOk = $true
            Write-Success "Server startup confirmed in log."
            break
        }
    }

    if ($i % 10 -eq 0) {
        Write-Info "  Still waiting... ($i seconds)"
    }
}

if (-not $startupOk) {
    if ($javaProcess.HasExited) {
        Write-Err "Server died. Check logs."
    }
    Write-Warn "Could not confirm startup. Check $LogFile."
}

# ── DB check ────────────────────────────────────────────────────────────────────
Write-Info "Checking database..."
$dbOk = $false
if (Test-Path $LogFile) {
    $logContent = Get-Content $LogFile -Raw -ErrorAction SilentlyContinue
    if ($logContent -match "\[Database\] Connected to") {
        Write-Success "Database: $($Matches[0])"
        $dbOk = $true
    } elseif ($logContent -match "Connected successfully") {
        Write-Success "Database: Connected successfully"
        $dbOk = $true
    } elseif ($logContent -match "Could not connect|Connection refused|Communications link|Unable to initialize the database") {
        $dbErr = [regex]::Matches($logContent, "Could not connect.*|Connection refused.*|Communications link.*|Unable to initialize the database.*") | Select-Object -First 1
        Write-Err "Database connection FAILED: $($dbErr.Value)"
    }
}
if (-not $dbOk) {
    Write-Warn "DB not confirmed. Check $LogFile."
}

# ── Error scan ─────────────────────────────────────────────────────────────────
if (Test-Path $LogFile) {
    $logContent = Get-Content $LogFile -Raw -ErrorAction SilentlyContinue
    $errors = [regex]::Matches($logContent, "(?i)Exception|ERROR|FATAL") |
        Where-Object { $_.Value -notmatch "junit|concurrent|DEBUG|FINE|Client handling error|\[Database\]" } |
        Select-Object -First 10

    if ($errors) {
        Write-Warn "Errors found in log:"
        foreach ($e in $errors) {
            Write-Host "  $($e.Value)" -ForegroundColor Red
        }
    }
}

# ── Final ─────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Success "=========================================="
Write-Success "  Server restarted!"
Write-Success "  PID:   $javaPid"
Write-Success "  Port:  ${BindHost}:${ServerPort}"
Write-Success "  Log:   $LogFile"
if ($dbOk) { Write-Success "  DB:    Connected" } else { Write-Warn "  DB:    Not confirmed" }
Write-Success "=========================================="
Write-Host ""
Write-Info "To follow logs:   Get-Content $LogFile -Wait -Tail 30"
Write-Info "To stop server:   Stop-Process -Id $javaPid -Force"
Write-Info "To redeploy:     .\$($MyInvocation.MyCommand.Name)"
Write-Host ""
