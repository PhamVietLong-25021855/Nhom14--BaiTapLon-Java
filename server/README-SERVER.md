# Server

Server là tiến trình duy nhất truy cập database. Client gửi `AuctionRequest` qua socket, server xử lý bằng controller/service/DAO và trả `AuctionResponse`.

## Build server

Từ root project:

```bash
mvn -ntp -pl server -am package -DskipTests
```

File chạy chính sau build:

```text
server/target/server-1.0.0-SNAPSHOT.jar
```

## Chạy server

PowerShell:

```powershell
$env:DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server\target\server-1.0.0-SNAPSHOT.jar
```

Bash/Linux:

```bash
export DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

## Cổng và database

- App server mặc định dùng TCP `5050`.
- MySQL config nằm ở `server/src/main/resources/database.properties`.
- Không commit mật khẩu thật; truyền bằng `DB_PASSWORD` hoặc JVM property.
