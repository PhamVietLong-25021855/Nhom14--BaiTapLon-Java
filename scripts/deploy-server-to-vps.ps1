param(
    [string]$VpsHost = "172.104.50.54",
    [string]$VpsUser = "root",
    [string]$RemoteDir = "/root/auction-server"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

Push-Location $root
try {
    powershell -ExecutionPolicy Bypass -File ".\scripts\split-client-server.ps1" -OutputDir "dist" -ServerFolderName "server" -ClientFolderName "client"

    Write-Host "Creating $RemoteDir on $VpsHost..."
    ssh "$VpsUser@$VpsHost" "mkdir -p '$RemoteDir'"

    Write-Host "Uploading dist/server to ${VpsHost}:$RemoteDir..."
    scp -r ".\dist\server\*" "$VpsUser@$VpsHost`:$RemoteDir/"

    Write-Host ""
    Write-Host "Done. SSH into the VPS and start the server:"
    Write-Host "ssh $VpsUser@$VpsHost"
    Write-Host "cd $RemoteDir"
    Write-Host "export DB_PASSWORD='mat_khau_database_cua_ban'"
    Write-Host "java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp `"target/classes:target/dependency/*`" userauth.server.AuctionServerMain"
} finally {
    Pop-Location
}
