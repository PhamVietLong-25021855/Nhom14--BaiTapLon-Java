# Deploy Guide

Tài liệu này là checklist ngắn để deploy server lên VPS và chạy client từ máy khác.

## 1. Chuẩn bị

- JDK 21.
- Maven 3.6.3 trở lên.
- MySQL/Akamai DB đã cho phép IP của VPS truy cập.
- TCP `5050` mở trên firewall nếu client kết nối trực tiếp.

## 2. Build server

```bash
mvn -ntp -pl server -am package -DskipTests
```

## 3. Chạy server trên VPS

```bash
export DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

Nếu dùng port khác, đổi cả `app.server.port` trên server và `-ServerPort` ở client.

## 4. Chạy client

```powershell
cd client
.\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

## 5. Kiểm tra nhanh

| Việc cần kiểm tra | Lệnh/gợi ý |
| --- | --- |
| Server có chạy không | Xem log terminal server |
| Port có mở không | `Test-NetConnection IP_PUBLIC -Port 5050` |
| Database kết nối được không | Kiểm tra `DB_PASSWORD`, trusted sources và log server |
| Client trỏ đúng server không | Kiểm tra `-ServerHost`, `APP_SERVER_HOST` |

## 6. SSH tunnel

Nếu không mở port app public, giữ server chạy trên VPS và tạo tunnel:

```powershell
cd client
.\run-client-via-ssh.ps1
```

Client sẽ kết nối local port, còn traffic đi qua SSH.
