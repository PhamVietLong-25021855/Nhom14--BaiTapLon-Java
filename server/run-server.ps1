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

mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

mvn dependency:copy-dependencies -DincludeScope=runtime
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

java "-Dapp.server.port=$ServerPort" "-Dapp.server.bind.host=$BindHost" -cp "target/classes;target/dependency/*" userauth.server.AuctionServerMain
