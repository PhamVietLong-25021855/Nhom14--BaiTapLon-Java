# Server riêng cho hệ thống đấu giá

Server là tiến trình duy nhất được phép truy cập database. Client JavaFX chạy ở chế độ remote chỉ gọi server qua Socket.

## Chạy server trên VPS

```bash
export DB_PASSWORD="mat_khau_database_cua_ban"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Chạy server trên Windows (PowerShell)

```powershell
.\server\run-server.ps1 -DbPassword "mat_khau_database_cua_ban" -ServerPort 5050
```

Nếu dùng Maven:

```bash
DB_PASSWORD="mat_khau_database_cua_ban" mvn -DskipTests package
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Port cần mở trên firewall

Mở TCP `5050` cho client truy cập. Database Akamai vẫn chỉ cần server truy cập qua host/port MySQL.

Server mac dinh bind `0.0.0.0`, tuc la nghe tren moi card mang cua may server. Client o mang khac van can ket noi den public IP/domain cua server va firewall/VPS firewall phai mo TCP `5050`.
