param(
    [string]$OutputDir = "dist",
    [string]$ServerFolderName = "server",
    [string]$ClientFolderName = "client"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "use-jdk21.ps1")
$distRoot = Join-Path $root $OutputDir
$serverDir = Join-Path $distRoot $ServerFolderName
$clientDir = Join-Path $distRoot $ClientFolderName
$classesRoot = Join-Path $root "target\classes"

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

function Copy-RuntimePath {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RelativePath,
        [Parameter(Mandatory = $true)]
        [string]$DestinationRoot
    )

    $sourcePath = Join-Path $classesRoot $RelativePath
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Runtime path not found: $sourcePath"
    }

    $destinationClasses = Join-Path $DestinationRoot "target\classes"
    $destinationPath = Join-Path $destinationClasses $RelativePath
    $destinationParent = Split-Path -Parent $destinationPath
    New-Item -ItemType Directory -Force $destinationParent | Out-Null
    Copy-Item -LiteralPath $sourcePath -Destination $destinationParent -Recurse -Force
}

function Assert-MissingPaths {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PackageRoot,
        [Parameter(Mandatory = $true)]
        [string[]]$RelativePaths
    )

    foreach ($relativePath in $RelativePaths) {
        $path = Join-Path $PackageRoot $relativePath
        if (Test-Path $path) {
            throw "Forbidden runtime content found: $path"
        }
    }
}

Assert-UnderPath -Path $distRoot -ParentPath $root

Write-Host "[1/5] Building project..."
Push-Location $root
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }

Write-Host "[2/5] Copying runtime dependencies..."
$targetDependencyDir = Join-Path $root "target\dependency"
if (Test-Path -LiteralPath $targetDependencyDir) {
    Assert-UnderPath -Path $targetDependencyDir -ParentPath $root
    Remove-Item -LiteralPath $targetDependencyDir -Recurse -Force
}
mvn dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }
Pop-Location

Write-Host "[3/5] Creating split directories..."
Reset-Directory -Path $serverDir -ParentPath $distRoot
Reset-Directory -Path $clientDir -ParentPath $distRoot
New-Item -ItemType Directory -Force (Join-Path $serverDir "target\classes") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $serverDir "target\dependency") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $clientDir "target\classes") | Out-Null
New-Item -ItemType Directory -Force (Join-Path $clientDir "target\dependency") | Out-Null

Write-Host "[4/5] Copying server and client runtime classes..."
$serverRuntimePaths = @(
# Compiled from core-common module:
    "core-common\target\classes\userauth\api",
    "core-common\target\classes\userauth\common",
    "core-common\target\classes\userauth\network",
    "core-common\target\classes\userauth\util",
    "core-common\target\classes\userauth\validation",
    "core-common\target\classes\userauth\event",
    "core-common\target\classes\userauth\exception",
    "core-common\target\classes\userauth\model",

    # Compiled from server module:
    "server\target\classes\userauth\controller", # Backend controllers only
    "server\target\classes\userauth\dao",
    "server\target\classes\userauth\database",
    "server\target\classes\database.properties",  # Placed root-level in resources classpath
    "server\target\classes\userauth\server",
    "server\target\classes\userauth\service"
)

$clientRuntimePaths = @(
# Compiled from core-common module:
    "core-common\target\classes\userauth\api",
    "core-common\target\classes\userauth\common",
    "core-common\target\classes\userauth\network",
    "core-common\target\classes\userauth\util",
    "core-common\target\classes\userauth\validation",
    "core-common\target\classes\userauth\event",
    "core-common\target\classes\userauth\exception",
    "core-common\target\classes\userauth\model",

    # Compiled from client module:
    "client\target\classes\userauth\ClientLauncher.class",
    "client\target\classes\userauth\ClientMain.class",
    "client\target\classes\userauth\client",
    "client\target\classes\userauth\controller", # JavaFX UI controllers only
    "client\target\classes\userauth\gui"          # Contains your loaded FXML resources
)

foreach ($relativePath in $serverRuntimePaths) {
    Copy-RuntimePath -RelativePath $relativePath -DestinationRoot $serverDir
}

foreach ($relativePath in $clientRuntimePaths) {
    Copy-RuntimePath -RelativePath $relativePath -DestinationRoot $clientDir
}

Copy-DependenciesByPattern -Destination (Join-Path $serverDir "target\dependency") -Patterns @(
    "mysql-connector-j-*.jar",
    "protobuf-java-*.jar"
)
Copy-DependenciesByPattern -Destination (Join-Path $clientDir "target\dependency") -Patterns @(
    "javafx-*.jar"
)

Write-Host "[5/5] Writing launch scripts and validating package boundaries..."
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

java -Dapp.server.host="$ServerHost" -Dapp.server.port="$ServerPort" -cp "target/classes;target/dependency/*" userauth.ClientLauncher
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

java -Dapp.server.host="$LocalHost" -Dapp.server.port="$LocalPort" -cp "target/classes;target/dependency/*" userauth.ClientLauncher
'@

Set-Content -Path (Join-Path $serverDir "run-server.ps1") -Value $serverRun -Encoding UTF8
Set-Content -Path (Join-Path $clientDir "run-client.ps1") -Value $clientRun -Encoding UTF8
Set-Content -Path (Join-Path $clientDir "run-client-via-ssh.ps1") -Value $clientTunnelRun -Encoding UTF8

Set-Content -Path (Join-Path $serverDir ".env.example") -Value "DB_PASSWORD=your_database_password`nAPP_SERVER_PORT=5050`nAPP_SERVER_BIND_HOST=0.0.0.0`n" -Encoding UTF8
Set-Content -Path (Join-Path $clientDir ".env.example") -Value "APP_SERVER_HOST=172.104.50.54`nAPP_SERVER_PORT=5050`n" -Encoding UTF8

Assert-MissingPaths -PackageRoot $clientDir -RelativePaths @(
    "target\classes\userauth\dao",
    "target\classes\userauth\database",
    "target\classes\userauth\database.properties",
    "target\classes\userauth\server",
    "target\classes\userauth\service",
    "target\classes\userauth\Launcher.class",
    "target\classes\userauth\Main.class",
    "target\dependency\mysql-connector-j-*.jar",
    "target\dependency\protobuf-java-*.jar"
)

Assert-MissingPaths -PackageRoot $serverDir -RelativePaths @(
    "target\classes\userauth\ClientLauncher.class",
    "target\classes\userauth\ClientMain.class",
    "target\classes\userauth\client",
    "target\classes\userauth\gui",
    "target\classes\userauth\Launcher.class",
    "target\classes\userauth\Main.class",
    "target\dependency\javafx-*.jar"
)

Write-Host ""
Write-Host "Split package created successfully:"
Write-Host "  - $serverDir"
Write-Host "  - $clientDir"
