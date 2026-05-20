# Client JavaFX

Client chỉ hiển thị giao diện và gọi server qua socket. Client không truy cập database, không dùng DAO và không giữ mật khẩu database.

## Chạy client

Từ thư mục `client`:

```powershell
cd client
.\run-client.ps1 -ServerHost "127.0.0.1" -ServerPort 5050
```

Nếu không truyền `-ServerHost`, script sẽ đọc `APP_SERVER_HOST`; nếu biến này không có, mặc định dùng `172.104.50.54`.

## Chạy bằng Maven từ root

```powershell
mvn -pl client javafx:run "-Dmain.class=userauth.ClientLauncher" "-Dapp.server.host=127.0.0.1" "-Dapp.server.port=5050"
```

## Chạy qua SSH tunnel

Dùng khi VPS chỉ mở SSH hoặc không muốn mở public port cho app:

```powershell
cd client
.\run-client-via-ssh.ps1
```

Giữ cửa sổ SSH mở trong lúc dùng client.
