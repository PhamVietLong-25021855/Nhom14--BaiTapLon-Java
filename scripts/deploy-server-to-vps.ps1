param(
    [string]$VpsHost = "172.104.50.54",
    [string]$VpsUser = "root",
    [string]$RemoteDir = "/root/auction-server",
    [string]$DbPassword
)

$ErrorActionPreference = "Stop"

if (-not $DbPassword) {
    $DbPassword = $env:DB_PASSWORD
}
if (-not $DbPassword) {
    Write-Host "WARNING: DB_PASSWORD not set. Server may fail to connect." -ForegroundColor Yellow
}

$root = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot "use-jdk21.ps1")

Push-Location $root
try {
    Write-Host "Building server module..."
    mvn package -pl server -am -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Host "Maven build failed." -ForegroundColor Red; exit 1 }

    Write-Host "Creating $RemoteDir on $VpsHost..."
    ssh "$VpsUser@$VpsHost" "mkdir -p '$RemoteDir'"

    Write-Host "Uploading server to ${VpsHost}:$RemoteDir..."
    scp -r ".\server\*" "$VpsUser@$VpsHost`:$RemoteDir/"

    if ($DbPassword) {
        Write-Host "Injecting DB password..."
        $escaped = $DbPassword -replace '[\/&]', '\$0'
        ssh "$VpsUser@$VpsHost" "sed -i 's/^db\.password=.*/db.password=$escaped/' '$RemoteDir/src/main/resources/userauth/database.properties'"
    }

    Write-Host ""
    Write-Host "Done. Server deployed to ${VpsHost}:$RemoteDir"
    Write-Host "To start: ssh $VpsUser@$VpsHost" '"'"'cd $RemoteDir && java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp target/auction-server.jar userauth.server.AuctionServerMain'"'"'
} finally {
    Pop-Location
}
