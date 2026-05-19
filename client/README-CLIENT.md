# Client JavaFX riêng

Client không truy cập database trực tiếp. Script client dùng `userauth.ClientLauncher`, chỉ khởi tạo remote service và gọi server qua Socket.

## Chạy client trên máy người dùng

Client mặc định trỏ tới VPS `172.104.50.54:5050`:

```powershell
.\client\run-client.ps1
```

Nếu đổi VPS hoặc dùng domain server:

```powershell
.\client\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

Hoặc truyền VM options trong IntelliJ:

```text
-Dapp.server.host=172.104.50.54 -Dapp.server.port=5050
```

## Chạy qua SSH port 22

Không chạy `AuctionServerMain` trực tiếp trên cổng `22` vì cổng này đang dùng cho SSH. Nếu VPS chỉ mở cổng `22`, hãy chạy app server trên VPS ở `127.0.0.1:5050` hoặc `0.0.0.0:5050`, rồi dùng SSH tunnel:

```powershell
.\client\run-client-via-ssh.ps1
```

Khi cửa sổ SSH hiện ra, đăng nhập và giữ cửa sổ đó mở. Client sẽ kết nối tới `127.0.0.1:5050`, còn đường mạng thật sẽ đi qua SSH port `22`.

Nếu cần chạy lại chế độ local DB cũ trong source project, dùng script gốc ở thư mục root:

```powershell
.\run-javafx.ps1 -LocalMode
```
