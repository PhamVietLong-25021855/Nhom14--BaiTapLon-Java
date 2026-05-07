# Server riêng cho hệ thống đấu giá

Server là tiến trình duy nhất được phép truy cập database. Client JavaFX chạy ở chế độ remote và chỉ gọi server qua Socket.

## Chạy server trên VPS

```bash
export DB_PASSWORD="mat_khau_database_cua_ban"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Chạy server trên Windows

```powershell
.\server\run-server.ps1 -DbPassword "mat_khau_database_cua_ban" -ServerPort 5050
```

Nếu dùng Maven:

```bash
DB_PASSWORD="mat_khau_database_cua_ban" mvn -DskipTests package
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Cổng cần mở trên firewall

Mở TCP `5050` để client truy cập server. Database Akamai/MySQL chỉ cần server truy cập qua host/port MySQL.

Mặc định server bind `0.0.0.0`, tức là lắng nghe trên mọi card mạng của máy server. Nếu client ở mạng khác, client vẫn cần kết nối tới public IP/domain của server và firewall/VPS firewall phải mở TCP `5050`.
