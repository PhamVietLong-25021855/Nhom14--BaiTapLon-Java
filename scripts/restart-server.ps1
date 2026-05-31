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
    [switch]$SkipTests,

    [Parameter(Mandatory = $false)]
    [switch]$Tls,

    [Parameter(Mandatory = $false)]
    [string]$KeyStore,

    [Parameter(Mandatory = $false)]
    [string]$KeyStorePassword
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$serverScript = Join-Path $projectRoot "server\run-server.ps1"

$arguments = @{
    ServerPort = $ServerPort
    BindHost = $BindHost
}
if ($DbPassword) { $arguments.DbPassword = $DbPassword }
if ($SkipBuild) { $arguments.SkipBuild = $true }
if ($SkipTests) { $arguments.SkipTests = $true }
if ($Tls) { $arguments.Tls = $true }
if ($KeyStore) { $arguments.KeyStore = $KeyStore }
if ($KeyStorePassword) { $arguments.KeyStorePassword = $KeyStorePassword }

& $serverScript @arguments
exit $LASTEXITCODE
