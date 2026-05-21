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

## JDK 25 / IntelliJ warnings

This project targets Java 21 and JavaFX 21. If IntelliJ runs
`userauth.ClientLauncher` directly with JDK 25, JavaFX can print warnings about
unnamed modules, native access, or `sun.misc.Unsafe`. They are JVM/runtime
warnings, not server connection errors.

Recommended: set the IntelliJ Project SDK and Run Configuration JRE to JDK 21.

If you keep JDK 25, add this VM option to the IntelliJ Application run
configuration:

```text
--enable-native-access=ALL-UNNAMED
```

To avoid the `Unsupported JavaFX configuration` warning too, run the client via
the Maven JavaFX goal or the PowerShell script instead of IntelliJ's direct
classpath launcher.
