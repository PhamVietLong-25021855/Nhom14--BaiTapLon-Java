param(
    [string]$ServerHost = "",
    [int]$ServerPort = 5050
)

if (-not $ServerHost) {
    $ServerHost = $env:APP_SERVER_HOST
}
if (-not $ServerHost) {
    $ServerHost = "172.104.50.54"
}

mvn javafx:run "-Dmain.class=userauth.ClientLauncher" "-Dapp.server.host=$ServerHost" "-Dapp.server.port=$ServerPort"
