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
$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $repoRoot "scripts\use-jdk21.ps1")
Set-Location $PSScriptRoot

$mavenCommand = Join-Path $repoRoot "mvnw.cmd"
if (-not (Test-Path -LiteralPath $mavenCommand -PathType Leaf)) {
    $mavenCommand = (Get-Command mvn -ErrorAction SilentlyContinue).Source
}
if (-not $mavenCommand) {
    throw "Maven Wrapper and Maven were not found. Keep mvnw.cmd in the ZIP or install Maven 3.6.3 or newer."
}

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

& $mavenCommand clean javafx:run "-Dapp.server.host=$LocalHost" "-Dapp.server.port=$LocalPort"
exit $LASTEXITCODE
