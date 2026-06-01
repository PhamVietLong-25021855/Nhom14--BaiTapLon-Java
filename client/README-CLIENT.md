# Client JavaFX

Client chỉ hiển thị giao diện và gọi server qua socket. Client không truy cập database, không dùng DAO và không giữ mật khẩu database.

## Chạy client

Từ thư mục `client`:

```powershell
cd client
.\run-client.ps1 -ServerHost "127.0.0.1" -ServerPort 5050
```

Nếu không truyền `-ServerHost`, script sẽ đọc `APP_SERVER_HOST`; nếu biến này không có, mặc định dùng `172.104.50.54`.

## Chạy sau khi gửi file ZIP sang máy Windows khác

Máy nhận ZIP chỉ cần cài JDK 21 và có Internet trong lần chạy đầu tiên.
Không cần cài Maven hoặc tải riêng JavaFX SDK: Maven Wrapper sẽ tải Maven 3.9.9, sau đó Maven tải plugin và các thư viện JavaFX 21 được khai báo trong `client/pom.xml`.

Từ thư mục gốc sau khi giải nén:

```powershell
.\run-javafx.cmd
```

File `run-javafx.cmd` sẽ gọi PowerShell với `-ExecutionPolicy Bypass`, tránh lỗi chữ ký số khi chạy script trên máy mới.
Nếu JDK 21 nằm ở thư mục riêng, đặt `JDK21_HOME` hoặc `JAVA_HOME` trước khi chạy script.

Trong IntelliJ, chọn run configuration Maven có tên `ClientLauncher`. Cấu hình này chạy `clean javafx:run`, nên Maven sẽ build lại client trước khi mở giao diện.
Không bấm trực tiếp nút Run cạnh hàm `main()` trong `ClientLauncher.java` trên máy mới, vì đó là Java Application runner và không thể gọi Maven trước khi compile.

## Chạy bằng Maven từ thư mục gốc

```powershell
.\mvnw.cmd -f client/pom.xml clean javafx:run "-Dapp.server.host=127.0.0.1" "-Dapp.server.port=5050"
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
