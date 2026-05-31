param(
    [string]$ServerHost = "",
    [int]$ServerPort = 5050,
    [string]$ClientMode = "remote",
    [switch]$Tls,
    [string]$TrustStore = "",
    [string]$TrustStorePassword = ""
)

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $repoRoot "scripts\use-jdk21.ps1")
Set-Location $PSScriptRoot

if (-not $ServerHost) {
    $ServerHost = $env:APP_SERVER_HOST
}
if (-not $ServerHost) {
    $ServerHost = "172.104.50.54"
}

$mvnArgs = @(
    "clean",
    "javafx:run",
    "-Dapp.server.host=$ServerHost",
    "-Dapp.server.port=$ServerPort",
    "-Dapp.client.mode=$ClientMode"
)
if ($Tls) {
    $mvnArgs += "-Dapp.server.tls.enabled=true"
}
if ($TrustStore) {
    $mvnArgs += "-Djavax.net.ssl.trustStore=$TrustStore"
}
if ($TrustStorePassword) {
    $mvnArgs += "-Djavax.net.ssl.trustStorePassword=$TrustStorePassword"
}

mvn @mvnArgs
