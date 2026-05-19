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
    [switch]$DisableScheduler
)

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if ($LocalMode) {
    $ClientMode = "local"
}

$mvnArgs = @("javafx:run")
$mainClass = "userauth.ClientLauncher"
if ($ClientMode -ieq "local") {
    $mainClass = "userauth.Launcher"
}
$mvnArgs += "-Dmain.class=$mainClass"
$mvnArgs += "-Dapp.client.mode=$ClientMode"

if (-not $ServerHost -and $ClientMode -ieq "remote") {
    $ServerHost = "172.104.50.54"
}
if ($ServerHost) {
    $mvnArgs += "-Dapp.server.host=$ServerHost"
}
$mvnArgs += "-Dapp.server.port=$ServerPort"

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

& mvn @mvnArgs
exit $LASTEXITCODE
