param(
    [string]$ServerHost,
    [int]$ServerPort = 5050,
    [string]$ClientMode = "remote",
    [switch]$LocalMode,
    [string]$DbUrl,
    [string]$DbAdminUrl,
    [string]$DbHost,
    [int]$DbPort,
    [string]$DbName,
    [string]$DbUser,
    [string]$DbPassword,
    [string]$DbSslMode,
    [string]$DbSchema,
    [switch]$DisableScheduler,
    [switch]$Tls,
    [string]$TrustStore,
    [string]$TrustStorePassword
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
. (Join-Path $root "scripts\use-jdk21.ps1")

if ($LocalMode) {
    throw "LocalMode is no longer supported by ClientLauncher. Start the server separately and run the remote client."
}

if ($ClientMode -ieq "local") {
    throw "ClientMode 'local' is no longer supported by ClientLauncher. Use ClientMode 'remote'."
}
$mavenCommand = Join-Path $root "mvnw.cmd"
if (-not (Test-Path -LiteralPath $mavenCommand -PathType Leaf)) {
    $mavenCommand = (Get-Command mvn.cmd -ErrorAction SilentlyContinue).Source
}
if (-not $mavenCommand) {
    $mavenCommand = (Get-Command mvn -ErrorAction SilentlyContinue).Source
}
if (-not $mavenCommand) {
    throw "Maven Wrapper and Maven were not found. Keep mvnw.cmd and .mvn/wrapper in the ZIP, or install Maven 3.6.3 or newer."
}

$clientPom = Join-Path $root "client\pom.xml"
$mvnArgs = @("-f", $clientPom, "compile", "javafx:run")
$mvnArgs += "-Dapp.client.mode=$ClientMode"

if (-not $ServerHost -and $ClientMode -ieq "remote") {
    $ServerHost = $env:APP_SERVER_HOST
}
if (-not $ServerHost -and $ClientMode -ieq "remote") {
    $ServerHost = "172.104.50.54"
}
if ($ServerHost) {
    $mvnArgs += "-Dapp.server.host=$ServerHost"
}
$mvnArgs += "-Dapp.server.port=$ServerPort"
if ($Tls) {
    $mvnArgs += "-Dapp.server.tls.enabled=true"
}
if ($TrustStore) {
    $mvnArgs += "-Djavax.net.ssl.trustStore=$TrustStore"
}
if ($TrustStorePassword) {
    $mvnArgs += "-Djavax.net.ssl.trustStorePassword=$TrustStorePassword"
}

if ($DbUrl) {
    $mvnArgs += "-Ddb.url=$DbUrl"
}
if ($DbAdminUrl) {
    $mvnArgs += "-Ddb.adminUrl=$DbAdminUrl"
}
if ($DbHost) {
    $mvnArgs += "-Ddb.host=$DbHost"
}
if ($DbPort) {
    $mvnArgs += "-Ddb.port=$DbPort"
}
if ($DbName) {
    $mvnArgs += "-Ddb.name=$DbName"
}
if ($DbUser) {
    $mvnArgs += "-Ddb.username=$DbUser"
}
if ($DbPassword) {
    $mvnArgs += "-Ddb.password=$DbPassword"
}
if ($DbSslMode) {
    $mvnArgs += "-Ddb.sslMode=$DbSslMode"
}
if ($DbSchema) {
    $mvnArgs += "-Ddb.schema=$DbSchema"
}
if ($DisableScheduler) {
    $mvnArgs += "-Dapp.scheduler.enabled=false"
}

& $mavenCommand @mvnArgs
exit $LASTEXITCODE
