# Nhóm 14 - Hệ thống đấu giá trực tuyến

Dự án sử dụng Java 25, JavaFX 25.0.2 và Maven. Bản nâng cấp hiện tại bổ sung kiến trúc Client-Server, hỗ trợ database MySQL trên Akamai/VPS và có bộ kiểm thử JUnit cho các logic quan trọng.

## Cấu trúc chính

```text
User/src/userauth/                 Mã nguồn chính của dự án
User/src/userauth/api/             Interface nghiệp vụ dùng chung cho client và server
User/src/userauth/common/          Hằng số/logic dùng chung, không phụ thuộc database
User/src/userauth/server/          Socket server chạy trên VPS hoặc máy host
User/src/userauth/client/remote/   Remote service phía client, không truy cập database trực tiếp
User/src/userauth/network/         Request/Response cho giao tiếp client-server
User/src/userauth/gui/fxml/        Controller JavaFX đã chia theo từng nhóm màn hình
User/resources/userauth/gui/fxml/  FXML/CSS đã chia theo từng nhóm giao diện
client/                            Script và hướng dẫn chạy client
server/                            Script và hướng dẫn chạy server
docs/                              Tài liệu kỹ thuật
src/test/java/                     Bộ kiểm thử JUnit
```

## Chạy server trên VPS

```bash
export DB_PASSWORD="mat_khau_database_cua_ban"
mvn -DskipTests package
mvn dependency:copy-dependencies -DincludeScope=runtime
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -cp "target/classes:target/dependency/*" userauth.server.AuctionServerMain
```

## Chạy server trên Windows

```powershell
.\server\run-server.ps1 -DbPassword "mat_khau_database_cua_ban" -ServerPort 5050
```

## Chạy client JavaFX

```powershell
.\client\run-client.ps1 -ServerHost "172.104.50.54" -ServerPort 5050
```

Client dùng entry point `userauth.ClientLauncher`, chỉ gọi server qua Socket và không khởi tạo DAO/database. Nếu đổi VPS hoặc domain, truyền lại `-ServerHost ...` hoặc đặt biến môi trường `APP_SERVER_HOST`.

Nếu VPS chỉ mở cổng SSH `22`, không chạy ứng dụng trực tiếp trên cổng `22`. Hãy chạy app server trên VPS ở cổng `5050`, sau đó chạy client qua SSH tunnel:

```powershell
.\client\run-client-via-ssh.ps1
```

## Tách client/server để deploy trên 2 máy

```powershell
.\scripts\split-client-server.ps1
```

Hướng dẫn chi tiết: `docs/split-client-server.md`

Gói `dist/client` được kiểm tra để không chứa `userauth/server`, `userauth/database`, `userauth/dao`, `userauth/service`, `database.properties` hoặc MySQL driver.

## Chạy kiểm thử

```bash
mvn test
```
