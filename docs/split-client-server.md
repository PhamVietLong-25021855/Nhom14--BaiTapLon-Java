# Tách client và server thành 2 bộ deploy riêng

Script `scripts/split-client-server.ps1` sẽ tạo:

- `dist/server`: dùng để copy lên máy chạy server.
- `dist/client`: dùng để copy sang máy người dùng.

Script copy theo danh sách package được phép thay vì copy toàn bộ `target/classes`.

## 1. Tạo gói tách riêng

```powershell
.\scripts\split-client-server.ps1
```

## 2. Chạy server trên máy host

Vào thư mục `dist/server`:

```powershell
.\run-server.ps1 -DbPassword "mat_khau_db" -ServerPort 5050
```

## 3. Chạy client trên máy khác

Vào thư mục `dist/client`:

```powershell
.\run-client.ps1 -ServerHost "IP_PUBLIC_CUA_SERVER" -ServerPort 5050
```

## Ghi chú

- Nếu client ở mạng khác, cần mở firewall hoặc port forwarding TCP `5050` trên máy server.
- Không commit mật khẩu thật vào source code.
- `dist/client` không chứa package server, DAO, database, service implementation, `database.properties` hoặc MySQL driver.
- `dist/server` không chứa JavaFX GUI/client và không copy dependency JavaFX.
