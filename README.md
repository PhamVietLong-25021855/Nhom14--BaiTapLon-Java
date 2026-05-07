# Nhom14 - Hệ thống đấu giá trực tuyến

Bản nâng cấp này bổ sung kiến trúc Client-Server, hỗ trợ Akamai MySQL, và thêm JUnit test cho các logic quan trọng.

## Cấu trúc quan trọng

```text
User/src/userauth/                 Source chính của project
User/src/userauth/server/          Server socket chạy trên VPS
User/src/userauth/client/remote/   Client remote service, không truy cập DB
User/src/userauth/network/         Request/Response giao tiếp client-server
client/                            Hướng dẫn/script chạy client
server/                            Hướng dẫn/script chạy server
docs/client-server-upgrade.md      Mô tả nâng cấp kiến trúc
src/test/java/                     JUnit test
```

## Chạy server trên VPS

```bash
export DB_PASSWORD="mat_khau_database_cua_ban"
mvn -DskipTests package
mvn dependency:copy-dependencies -DincludeScope=runtime
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Chạy server trên Windows (PowerShell)

```powershell
.\server\run-server.ps1 -DbPassword "mat_khau_database_cua_ban" -ServerPort 5050
```

## Chạy client JavaFX

```powershell
mvn javafx:run "-Dapp.client.mode=remote" "-Dapp.server.host=172.104.50.54" "-Dapp.server.port=5050"
```

Mac dinh `userauth.Launcher` chay remote client va tro toi `172.104.50.54:5050`. Neu doi VPS/domain, truyen lai `-Dapp.server.host=...` hoac bien moi truong `APP_SERVER_HOST`.

Neu VPS chi mo SSH port `22`, khong doi app sang listen port `22`. Chay app server tren VPS o port `5050`, roi chay client qua SSH tunnel:

```powershell
.\client\run-client-via-ssh.ps1
```

## Tach client/server de deploy 2 may

```powershell
.\scripts\split-client-server.ps1
```

Huong dan chi tiet: `docs/split-client-server.md`

## Chạy test

```bash
mvn test
```
