# Nhóm 14 - Hệ thống đấu giá trực tuyến

Dự án JavaFX mô phỏng hệ thống đấu giá trực tuyến theo kiến trúc client-server. Client chỉ hiển thị giao diện và gửi request qua socket; server xử lý nghiệp vụ, quản lý ví, đấu giá, tự động đặt giá và truy cập MySQL.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Build tool | Maven multi-module |
| Giao diện | JavaFX 21 |
| Database | MySQL/Akamai DB |
| Giao tiếp | Java Socket + request/response object |
| Kiểm thử | JUnit 5 |

## Chức năng chính

- Đăng ký, đăng nhập, đổi mật khẩu và cập nhật hồ sơ.
- Phân quyền `ADMIN`, `SELLER`, `BIDDER`.
- Seller tạo, sửa, xóa/hủy và đóng phiên đấu giá.
- Bidder xem phiên, đặt giá, dùng ví và cấu hình tự động đặt giá.
- Admin quản lý tài khoản, thông báo trang chủ và lệnh đóng sớm phiên đấu giá.
- Ví điện tử hỗ trợ nạp tiền, giữ tiền khi đang dẫn giá, hoàn tiền khi bị vượt giá và thu tiền khi phiên được xác nhận thanh toán.
- Server có cache ngắn hạn, index database và các test JUnit cho logic quan trọng.

## Cấu trúc project

```text
pom.xml                         Parent Maven project
core-common/                    Mã dùng chung cho client và server
  src/main/java/userauth/api     Interface nghiệp vụ
  src/main/java/userauth/model   Model domain
  src/main/java/userauth/network Request/response qua socket
  src/main/java/userauth/util    Tiện ích dùng chung
  src/main/java/userauth/validation
client/                         Ứng dụng JavaFX
  src/main/java/userauth/remote  Remote service gọi server
  src/main/java/userauth/gui     Controller JavaFX
  src/main/resources             FXML/CSS
server/                         Backend socket server
  src/main/java/userauth/server  Server main, socket, request handler
  src/main/java/userauth/service Nghiệp vụ
  src/main/java/userauth/dao     Truy cập database
  src/main/resources             Cấu hình database
docs/                           Tài liệu kỹ thuật rút gọn
scripts/                        Script hỗ trợ deploy/tách client-server
```

## Luồng xử lý

```text
JavaFX Client
  -> Remote*Service
  -> AuctionRequest qua socket
  -> AuctionRequestHandler
  -> Controller
  -> Service
  -> DAO
  -> MySQL
```

Server trả kết quả bằng `AuctionResponse`. Client không giữ mật khẩu database và không truy cập DAO trực tiếp.

## Cấu hình database

File cấu hình chính nằm tại:

```text
server/src/main/resources/database.properties
```

Mật khẩu không nên ghi cứng vào Git. Khi chạy server, truyền bằng biến môi trường hoặc JVM property:

```powershell
$env:DB_PASSWORD="mat_khau_database"
```

```bash
export DB_PASSWORD="mat_khau_database"
```

## Build và kiểm thử

Chạy toàn bộ test:

```bash
mvn -ntp test
```

Build toàn project:

```bash
mvn -ntp package
```

Build riêng server kèm module phụ thuộc:

```bash
mvn -ntp -pl server -am package -DskipTests
```

## Chạy server

Sau khi build, chạy server bằng shaded jar:

```powershell
$env:DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server\target\server-1.0.0-SNAPSHOT.jar
```

Trên Linux/VPS:

```bash
export DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

Port mặc định của server là `5050`. Nếu client chạy từ máy khác, cần mở TCP `5050` trên firewall/VPS.

## Chạy client

Từ thư mục `client`:

```powershell
cd client
.\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

Hoặc chạy Maven trực tiếp từ root:

```powershell
mvn -pl client javafx:run "-Dmain.class=userauth.ClientLauncher" "-Dapp.server.host=127.0.0.1" "-Dapp.server.port=5050"
```

Nếu server ở VPS nhưng chỉ mở SSH, dùng SSH tunnel trong `client/run-client-via-ssh.ps1`.

## Luồng ví và đặt giá

Ví có 3 số liệu chính:

| Trường | Ý nghĩa |
| --- | --- |
| `balance` | Tổng số tiền trong ví |
| `reservedBalance` | Tiền đang bị giữ cho giá dẫn đầu |
| `availableBalance` | Tiền còn có thể dùng, bằng `balance - reservedBalance` |

Ví dụ:

| Thao tác | Balance | Reserved | Available |
| --- | ---: | ---: | ---: |
| Nạp 1,000,000 | 1,000,000 | 0 | 1,000,000 |
| Đặt giá 500,000 | 1,000,000 | 500,000 | 500,000 |
| Bị người khác vượt giá | 1,000,000 | 0 | 1,000,000 |
| Thắng và được xác nhận PAID | 500,000 | 0 | 500,000 |

Các nghiệp vụ chính nằm trong `WalletService`: giữ tiền khi đặt giá, giải phóng tiền khi bị vượt, thu tiền khi thanh toán và hoàn tiền khi hủy.

## Tách client/server để deploy

Script hỗ trợ tạo 2 gói runtime riêng:

```powershell
.\scripts\split-client-server.ps1
```

Kết quả nằm trong `dist/server` và `dist/client`. Gói client không chứa DAO, service backend, database config hoặc MySQL driver.

## Tài liệu thêm

- `client/README-CLIENT.md`: cách chạy client.
- `server/README-SERVER.md`: cách chạy server.
- `docs/DEPLOY-GUIDE.md`: checklist deploy VPS ngắn gọn.
- `docs/optimization-guide.md`: các tối ưu hiệu năng đang dùng.
- `docs/design-patterns.md`: các design pattern chính.
- `docs/split-client-server.md`: cách tách gói client/server.

## Lỗi thường gặp

| Lỗi | Cách kiểm tra nhanh |
| --- | --- |
| Client không kết nối được server | Kiểm tra IP/domain, port `5050`, firewall và server log |
| Server không kết nối được database | Kiểm tra `DB_PASSWORD`, host/port MySQL và trusted sources |
| Không đặt giá được sau khi nạp tiền | Kiểm tra số dư ví, giá đặt có lớn hơn giá hiện tại không, và log `[Wallet]` |
| FXML không load | Chạy `mvn test` để kiểm tra resource/controller consistency |
