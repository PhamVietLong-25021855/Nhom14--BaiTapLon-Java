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
    Write-Warning "DB_PASSWORD chua duoc truyen. Hay truyen -DbPassword hoac set bien moi truong DB_PASSWORD truoc khi chay server."
}

$env:APP_SERVER_PORT = "$ServerPort"
$env:APP_SERVER_BIND_HOST = "$BindHost"

Push-Location $PSScriptRoot

Write-Host "Building server..."
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Pop-Location; exit $LASTEXITCODE }

Write-Host "Starting server on ${BindHost}:${ServerPort}..."

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

$javaProcess = Start-Process -FilePath $javaCmd[0] -ArgumentList $javaCmd[1..($javaCmd.Length-1)] -WorkingDirectory $PSScriptRoot -PassThru -NoNewWindow

Write-Host "Server started with PID $($javaProcess.Id)"
Write-Host "Log: $PSScriptRoot\logs\server.log"
Write-Host "To stop: Stop-Process -Id $($javaProcess.Id) -Force"
