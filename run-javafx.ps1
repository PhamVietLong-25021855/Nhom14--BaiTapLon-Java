param(
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
$javafxLib = Join-Path $root "javafx-sdk\lib"
$projectPostgresJar = Join-Path $root "lib\postgresql-42.7.5.jar"
$postgresRepo = Join-Path $env:USERPROFILE ".m2\repository\org\postgresql\postgresql"
$sourceRoot = Join-Path $root "User\src"
$resourceRoot = Join-Path $root "User\resources"
$outputDir = Join-Path $root "out\javafx-app"

if (-not (Test-Path $javafxLib)) {
    Write-Error "Khong tim thay JavaFX SDK tai $javafxLib"
    exit 1
}

$postgresJar = $null
if (Test-Path $projectPostgresJar) {
    $postgresJar = $projectPostgresJar
} else {
    $postgresJar = Get-ChildItem -Path $postgresRepo -Recurse -Filter "postgresql-*.jar" -File -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
}

if (-not $postgresJar) {
    Write-Error "Khong tim thay PostgreSQL JDBC driver trong $postgresRepo. Hay tai dependency Maven truoc."
    exit 1
}

New-Item -ItemType Directory -Force $outputDir | Out-Null
$files = Get-ChildItem -Path $sourceRoot -Recurse -Filter *.java | ForEach-Object { $_.FullName }

javac --module-path $javafxLib --add-modules javafx.controls,javafx.fxml -cp $postgresJar -d $outputDir $files
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

if (Test-Path $resourceRoot) {
    Get-ChildItem -Path $resourceRoot -Recurse -File | ForEach-Object {
        $relativePath = $_.FullName.Substring($resourceRoot.Length + 1)
        $destination = Join-Path $outputDir $relativePath
        $destinationDir = Split-Path -Parent $destination
        New-Item -ItemType Directory -Force $destinationDir | Out-Null
        Copy-Item $_.FullName $destination -Force
    }
}

$javaArgs = @(
    "--module-path", $javafxLib,
    "--add-modules", "javafx.controls,javafx.fxml",
    "--enable-native-access=javafx.graphics"
)

if ($DbUrl) {
    $javaArgs += "-Ddb.url=$DbUrl"
}
if ($DbAdminUrl) {
    $javaArgs += "-Ddb.adminUrl=$DbAdminUrl"
}
if ($DbHost) {
    $javaArgs += "-Ddb.host=$DbHost"
}
if ($DbPort) {
    $javaArgs += "-Ddb.port=$DbPort"
}
if ($DbName) {
    $javaArgs += "-Ddb.name=$DbName"
}
if ($DbUser) {
    $javaArgs += "-Ddb.username=$DbUser"
}
if ($DbPassword) {
    $javaArgs += "-Ddb.password=$DbPassword"
}
if ($DbSslMode) {
    $javaArgs += "-Ddb.sslMode=$DbSslMode"
}
if ($DbSchema) {
    $javaArgs += "-Ddb.schema=$DbSchema"
}
if ($DisableScheduler) {
    $javaArgs += "-Dapp.scheduler.enabled=false"
}

$javaArgs += @("-cp", "$outputDir;$postgresJar", "userauth.Main")

& java @javaArgs
exit $LASTEXITCODE
