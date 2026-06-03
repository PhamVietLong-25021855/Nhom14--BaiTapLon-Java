# Hướng Dẫn Chạy Client Cho Người Sử Dụng

Tài liệu này dành cho người dùng chỉ cần mở ứng dụng JavaFX để sử dụng hệ thống đấu giá. Nếu server đã được nhóm chạy sẵn trên VPS, bạn không cần chạy server và không cần cấu hình database.

## 1. Người dùng cần chuẩn bị gì?

Máy chạy client cần:

- JDK 21.
- Internet trong lần chạy đầu tiên để Maven Wrapper tải Maven/JavaFX.
- Source project hoặc file ZIP đã giải nén đầy đủ.
- Server đang chạy sẵn, mặc định tại `172.104.50.54:5050`.

Người dùng không cần:

- Không cần cài Maven thủ công nếu ZIP còn đủ `mvnw.cmd` và `.mvn/`.
- Không cần tải JavaFX SDK riêng.
- Không cần biết `DB_PASSWORD`.
- Không cần mở MySQL.
- Không cần chạy module `server` nếu server VPS đã hoạt động.

## 2. Cấu trúc file cần giữ khi gửi ZIP

Khi gửi project sang máy khác, cần giữ nguyên các file/thư mục sau:

```text
.mvn/
mvnw.cmd
mvnw
pom.xml
run-javafx.cmd
run-javafx.ps1
client/
core-common/
```

Nếu thiếu `.mvn/` hoặc `mvnw.cmd`, máy người dùng có thể không chạy được nếu chưa cài Maven.

## 3. Cách chạy nhanh trên Windows

Mở PowerShell hoặc Command Prompt tại thư mục gốc project, sau đó chạy:

```powershell
.\run-javafx.cmd
```

Lệnh này sẽ:

- Gọi PowerShell với `ExecutionPolicy Bypass`.
- Tự chọn JDK 21 nếu script tìm thấy.
- Dùng Maven Wrapper `mvnw.cmd`.
- Tải Maven/JavaFX nếu máy chưa có.
- Build `client` và `core-common`.
- Mở ứng dụng JavaFX bằng `userauth.ClientLauncher`.
- Mặc định kết nối server `172.104.50.54:5050`.

## 4. Chạy client với server host/port khác

Nếu server không chạy ở IP mặc định, truyền host và port:

```powershell
.\run-javafx.cmd -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

Ví dụ chạy với server local:

```powershell
.\run-javafx.cmd -ServerHost "127.0.0.1" -ServerPort 5050
```

Ví dụ chạy với VPS:

```powershell
.\run-javafx.cmd -ServerHost "172.104.50.54" -ServerPort 5050
```

## 5. Chạy bằng biến môi trường

Có thể cấu hình host/port bằng biến môi trường:

```powershell
$env:APP_SERVER_HOST="172.104.50.54"
$env:APP_SERVER_PORT="5050"
.\run-javafx.cmd
```

Cách này hữu ích nếu bạn không muốn nhập `-ServerHost` mỗi lần chạy.

## 6. Chạy bằng Maven Wrapper

Nếu muốn chạy trực tiếp bằng Maven Wrapper:

```powershell
.\mvnw.cmd -f client/pom.xml clean javafx:run "-Dapp.server.host=172.104.50.54" "-Dapp.server.port=5050"
```

Nếu chạy trên Linux/macOS:

```bash
./mvnw -f client/pom.xml clean javafx:run -Dapp.server.host=172.104.50.54 -Dapp.server.port=5050
```

Khuyến nghị cho người dùng Windows vẫn là dùng:

```powershell
.\run-javafx.cmd
```

vì script này đã xử lý nhiều chi tiết môi trường thay cho bạn.

## 7. Chạy từ folder `client`

Nếu đang đứng trong folder `client`, có thể dùng script riêng:

```powershell
cd client
.\run-client.ps1 -ServerHost "172.104.50.54" -ServerPort 5050
```

Nếu không truyền `-ServerHost`, script sẽ đọc `APP_SERVER_HOST`. Nếu biến môi trường này không có, script mặc định dùng `172.104.50.54`.

## 8. Chạy client qua SSH tunnel

Dùng cách này khi server không mở public port app hoặc muốn đi qua SSH.

```powershell
cd client
.\run-client-via-ssh.ps1
```

Script sẽ tạo tunnel:

```text
127.0.0.1:5050 -> VPS:127.0.0.1:5050
```

Trong lúc dùng client, giữ cửa sổ SSH tunnel mở. Nếu đóng tunnel, client sẽ mất kết nối server.

## 9. Khi nào cần chạy server?

Chỉ cần chạy server nếu:

- Bạn là người triển khai backend.
- Server VPS hiện chưa chạy.
- Bạn muốn test local trên máy cá nhân.
- Bạn thay đổi code server và cần kiểm tra lại.

Người sử dụng bình thường chỉ chạy client.

Nếu cần triển khai/chạy server, xem:

- [DEPLOY-GUIDE.md](DEPLOY-GUIDE.md)
- [../server/README-SERVER.md](../server/README-SERVER.md)

## 10. Lỗi thường gặp khi chạy client

| Lỗi | Cách xử lý |
| --- | --- |
| `java` không được nhận diện | Cài JDK 21 và kiểm tra `java -version`. |
| JavaFX không mở | Chạy bằng `.\run-javafx.cmd` thay vì bấm trực tiếp `main()` trong IDE. |
| Không tải được dependency lần đầu | Kiểm tra Internet, proxy/firewall, sau đó chạy lại `.\run-javafx.cmd`. |
| Client không kết nối được server | Kiểm tra `ServerHost`, `ServerPort`, server có chạy không, port `5050` có mở không. |
| `Connection refused` | Server chưa chạy hoặc port sai. |
| `Connection timed out` | Sai IP/domain, firewall chặn, hoặc VPS không mở port. |
| `local class incompatible` hoặc `serialVersionUID` | Client và server lệch version code. Cần build/deploy lại server hoặc cập nhật client cùng bản code. |
| PowerShell chặn script | Chạy `.\run-javafx.cmd`, file này đã gọi PowerShell với `ExecutionPolicy Bypass`. |

## 11. Kiểm tra server có kết nối được không

Trên Windows PowerShell:

```powershell
Test-NetConnection 172.104.50.54 -Port 5050
```

Nếu `TcpTestSucceeded` là `True`, máy client nhìn thấy port server.

Nếu là `False`, kiểm tra:

- Server có đang chạy không.
- IP/domain có đúng không.
- Port có đúng không.
- Firewall/VPS security group có mở port không.

## 12. Tóm tắt nhanh cho người dùng

Nếu đã có ZIP/source đầy đủ và server VPS đã chạy:

```powershell
.\run-javafx.cmd
```

Nếu cần chỉ rõ server:

```powershell
.\run-javafx.cmd -ServerHost "172.104.50.54" -ServerPort 5050
```

Không cần chạy server, không cần cấu hình database.
