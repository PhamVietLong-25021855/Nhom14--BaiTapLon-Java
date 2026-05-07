param(
    [string]$OutputDir = ".",
    [string]$ServerFolderName = "server-app",
    [string]$ClientFolderName = "client-app"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$distRoot = Join-Path $root $OutputDir
$serverDir = Join-Path $distRoot $ServerFolderName
$clientDir = Join-Path $distRoot $ClientFolderName

function Assert-UnderPath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$ParentPath
    )

    $fullPath = [System.IO.Path]::GetFullPath($Path)
    $fullParentPath = [System.IO.Path]::GetFullPath($ParentPath)
    if (-not $fullParentPath.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $fullParentPath += [System.IO.Path]::DirectorySeparatorChar
    }

    $pathForCompare = $fullPath
    if (-not $pathForCompare.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $pathForCompare += [System.IO.Path]::DirectorySeparatorChar
    }

    if (-not $pathForCompare.StartsWith($fullParentPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to operate outside ${fullParentPath}: $fullPath"
    }
}

function Reset-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$ParentPath
    )

    Assert-UnderPath -Path $Path -ParentPath $ParentPath
    if (Test-Path -LiteralPath $Path) {
        Remove-Item -LiteralPath $Path -Recurse -Force
    }
    New-Item -ItemType Directory -Force $Path | Out-Null
}

function Copy-DependenciesByPattern {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Destination,
        [Parameter(Mandatory = $true)]
        [string[]]$Patterns
    )

    $source = Join-Path $root "target\dependency"
    foreach ($pattern in $Patterns) {
        Get-ChildItem -Path $source -Filter $pattern -File -ErrorAction SilentlyContinue |
            Copy-Item -Destination $Destination -Force
    }
}

Assert-UnderPath -Path $distRoot -ParentPath $root

Write-Host "[1/4] Building project..."
Push-Location $root
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }

Write-Host "[2/4] Copying runtime dependencies..."
$targetDependencyDir = Join-Path $root "target\dependency"
if (Test-Path -LiteralPath $targetDependencyDir) {
    Assert-UnderPath -Path $targetDependencyDir -ParentPath $root
    Remove-Item -LiteralPath $targetDependencyDir -Recurse -Force
}
mvn dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host "[3/4] Creating split directories..."
Reset-Directory -Path $serverDir -ParentPath $distRoot
Reset-Directory -Path $clientDir -ParentPath $distRoot
New-Item -ItemType Directory -Force (Join-Path $serverDir "target\classes") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $serverDir "target\dependency") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $clientDir "target\classes") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $clientDir "target\dependency") | Out-Null

Write-Host "[4/4] Copying files..."
Copy-Item -Path (Join-Path $root "target\classes\*") -Destination (Join-Path $serverDir "target\classes") -Recurse -Force
Copy-Item -Path (Join-Path $root "target\classes\*") -Destination (Join-Path $clientDir "target\classes") -Recurse -Force
Copy-DependenciesByPattern -Destination (Join-Path $serverDir "target\dependency") -Patterns @(
    "mysql-connector-j-*.jar",
    "protobuf-java-*.jar"
)
Copy-DependenciesByPattern -Destination (Join-Path $clientDir "target\dependency") -Patterns @(
    "javafx-*.jar"
)

$clientDatabaseConfig = Join-Path $clientDir "target\classes\userauth\database.properties"
if (Test-Path -LiteralPath $clientDatabaseConfig) {
    Remove-Item -LiteralPath $clientDatabaseConfig -Force
}

$serverRun = @'
param(
    [Parameter(Mandatory = $false)]
    [string]$DbPassword,
    [Parameter(Mandatory = $false)]
    [int]$ServerPort = 5050,
    [Parameter(Mandatory = $false)]
    [string]$BindHost = "0.0.0.0"
)

$ErrorActionPreference = "Stop"

if (-not $DbPassword) {
    $DbPassword = $env:DB_PASSWORD
}
if ($DbPassword) {
    $env:DB_PASSWORD = $DbPassword
} else {
    Write-Warning "DB_PASSWORD is not set. Set it before starting the server, or pass -DbPassword."
}

$env:APP_SERVER_PORT = "$ServerPort"
$env:APP_SERVER_BIND_HOST = "$BindHost"

java "-Dapp.server.port=$ServerPort" "-Dapp.server.bind.host=$BindHost" -cp "target/classes;target/dependency/*" userauth.server.AuctionServerMain
'@

$clientRun = @'
param(
    [string]$ServerHost = "",
    [int]$ServerPort = 5050
)

$ErrorActionPreference = "Stop"

if (-not $ServerHost) {
    $ServerHost = $env:APP_SERVER_HOST
}
if (-not $ServerHost) {
    $ServerHost = "172.104.50.54"
}

java -Dapp.client.mode=remote -Dapp.server.host="$ServerHost" -Dapp.server.port="$ServerPort" -cp "target/classes;target/dependency/*" userauth.Launcher
'@

$clientTunnelRun = @'
param(
    [string]$SshHost = "172.104.50.54",
    [string]$SshUser = "root",
    [int]$SshPort = 22,
    [string]$LocalHost = "127.0.0.1",
    [int]$LocalPort = 5050,
    [string]$RemoteHost = "127.0.0.1",
    [int]$RemotePort = 5050
)

$ErrorActionPreference = "Stop"

$existingTunnel = Get-NetTCPConnection -LocalPort $LocalPort -State Listen -ErrorAction SilentlyContinue
if ($existingTunnel) {
    $owner = Get-Process -Id $existingTunnel[0].OwningProcess -ErrorAction SilentlyContinue
    if ($owner -and $owner.ProcessName -notlike "ssh*") {
        throw "Local port $LocalPort is already used by $($owner.ProcessName) (PID $($owner.Id)). Stop the local server or choose another -LocalPort."
    }
} else {
    $forward = "$LocalHost`:$LocalPort`:$RemoteHost`:$RemotePort"
    $sshCommand = "ssh -N -L $forward -p $SshPort $SshUser@$SshHost"
    Start-Process -FilePath "powershell" -ArgumentList @("-NoExit", "-Command", $sshCommand) -WindowStyle Normal
    Start-Sleep -Seconds 3
}

$ready = Test-NetConnection $LocalHost -Port $LocalPort -InformationLevel Quiet
if (-not $ready) {
    throw "SSH tunnel is not ready. Keep the SSH tunnel window open and sign in if it asks for a password."
}

java -Dapp.client.mode=remote -Dapp.server.host="$LocalHost" -Dapp.server.port="$LocalPort" -cp "target/classes;target/dependency/*" userauth.Launcher
'@

Set-Content -Path (Join-Path $serverDir "run-server.ps1") -Value $serverRun -Encoding UTF8
Set-Content -Path (Join-Path $clientDir "run-client.ps1") -Value $clientRun -Encoding UTF8
Set-Content -Path (Join-Path $clientDir "run-client-via-ssh.ps1") -Value $clientTunnelRun -Encoding UTF8

Set-Content -Path (Join-Path $serverDir ".env.example") -Value "DB_PASSWORD=your_database_password`nAPP_SERVER_PORT=5050`nAPP_SERVER_BIND_HOST=0.0.0.0`n" -Encoding UTF8
Set-Content -Path (Join-Path $clientDir ".env.example") -Value "APP_SERVER_HOST=172.104.50.54`nAPP_SERVER_PORT=5050`n" -Encoding UTF8

Write-Host ""
Write-Host "Split package created successfully:"
Write-Host "  - $serverDir"
Write-Host "  - $clientDir"
