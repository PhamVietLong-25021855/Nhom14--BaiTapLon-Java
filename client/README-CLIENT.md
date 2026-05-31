# Client JavaFX

Client chỉ hiển thị giao diện và gọi server qua socket. Client không truy cập database, không dùng DAO và không giữ mật khẩu database.

## Chạy client

Từ thư mục `client`:

```powershell
cd client
.\run-client.ps1 -ServerHost "127.0.0.1" -ServerPort 5050
```

Nếu không truyền `-ServerHost`, script sẽ đọc `APP_SERVER_HOST`; nếu biến này không có, mặc định dùng `172.104.50.54`.

## Chạy bằng Maven từ thư mục gốc

```powershell
mvn -pl client javafx:run "-Dmain.class=userauth.ClientLauncher" "-Dapp.server.host=127.0.0.1" "-Dapp.server.port=5050"
```

## Chạy qua đường hầm SSH

Dùng khi VPS chỉ mở SSH hoặc không muốn mở public port cho app:

```powershell
cd client
.\run-client-via-ssh.ps1
```

Giữ cửa sổ SSH mở trong lúc dùng client.

## Cảnh báo JDK 25 / IntelliJ

Project này hướng tới Java 21 và JavaFX 21. Nếu IntelliJ chạy trực tiếp
`userauth.ClientLauncher` bằng JDK 25, JavaFX có thể in cảnh báo về module chưa
đặt tên, quyền truy cập native hoặc `sun.misc.Unsafe`. Đây là cảnh báo của JVM/môi trường chạy,
không phải lỗi kết nối server.

Khuyến nghị: đặt Project SDK của IntelliJ và JRE trong cấu hình chạy về JDK 21.

Nếu vẫn dùng JDK 25, thêm VM option sau vào cấu hình chạy Application của
IntelliJ:

```text
--enable-native-access=ALL-UNNAMED
```

Để tránh thêm cảnh báo `Unsupported JavaFX configuration`, nên chạy client qua
mục tiêu Maven JavaFX hoặc script PowerShell thay vì trình chạy classpath trực tiếp
của IntelliJ.
