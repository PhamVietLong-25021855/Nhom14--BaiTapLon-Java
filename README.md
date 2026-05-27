# Nhóm 14 - Hệ thống đấu giá trực tuyến

Dự án xây dựng hệ thống đấu giá trực tuyến theo kiến trúc client-server. Client là ứng dụng JavaFX dùng để hiển thị giao diện và gửi request qua socket; server xử lý nghiệp vụ đấu giá, tài khoản, ví điện tử, thông báo và truy cập cơ sở dữ liệu MySQL.

Phạm vi hệ thống tập trung vào các luồng chính: đăng ký/đăng nhập, phân quyền người dùng, quản lý phiên đấu giá, đặt giá, tự động đặt giá, quản lý ví, thông báo và các thao tác quản trị cơ bản.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Build tool | Maven multi-module |
| Giao diện | JavaFX 21, FXML, CSS |
| Backend | Java Socket Server |
| Database | MySQL / Akamai DB |
| Giao tiếp client-server | Object request/response qua socket |
| Kiểm thử | JUnit 5 |

## Môi trường và yêu cầu cài đặt

- JDK 21 trở lên.
- Maven 3.6.3 trở lên.
- MySQL hoặc Akamai DB có thể truy cập từ máy chạy server.
- Port TCP `5050` được mở nếu client kết nối đến server từ máy khác.
- Biến môi trường `DB_PASSWORD` hoặc JVM property `-Ddb.password=...` để server kết nối database.

Kiểm tra phiên bản:

```bash
java -version
mvn -version
```

## Cấu trúc thư mục

```text
.
|-- pom.xml                         Parent Maven project
|-- core-common/                    Mã dùng chung cho client và server
|   |-- src/main/java/userauth/api
|   |-- src/main/java/userauth/model
|   |-- src/main/java/userauth/network
|   |-- src/main/java/userauth/util
|   `-- src/main/java/userauth/validation
|-- server/                         Backend socket server
|   |-- src/main/java/userauth/server
|   |-- src/main/java/userauth/service
|   |-- src/main/java/userauth/dao
|   |-- src/main/java/userauth/database
|   `-- src/main/resources/database.properties
|-- client/                         Ứng dụng JavaFX
|   |-- src/main/java/userauth/remote
|   |-- src/main/java/userauth/gui
|   `-- src/main/resources
|-- docs/                           Tài liệu thiết kế, deploy, tối ưu
|-- scripts/                        Script hỗ trợ deploy và tách gói
`-- database_indexes.sql            Script bổ sung index database
```

## Cấu hình database

File cấu hình mặc định:

```text
server/src/main/resources/database.properties
```

Mật khẩu database không nên ghi trực tiếp vào source code. Khi chạy server, đặt biến môi trường:

Windows PowerShell:

```powershell
$env:DB_PASSWORD="mat_khau_database"
```

Linux/macOS:

```bash
export DB_PASSWORD="mat_khau_database"
```

Có thể truyền trực tiếp bằng JVM property:

```bash
java -Ddb.password="mat_khau_database" ...
```

## Build và kiểm thử

Chạy từ thư mục gốc của project.

Chạy toàn bộ test:

```bash
mvn -ntp test
```

Build toàn bộ project:

```bash
mvn -ntp package
```

Build server kèm các module phụ thuộc:

```bash
mvn -ntp -pl server -am package -DskipTests
```

Build client kèm các module phụ thuộc:

```bash
mvn -ntp -pl client -am package -DskipTests
```

## Thứ tự chạy Server/Client

### 1. Chạy server trước

Build server:

```bash
mvn -ntp -pl server -am package -DskipTests
```

Windows PowerShell:

```powershell
$env:DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

Linux/macOS:

```bash
export DB_PASSWORD="mat_khau_database"
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

Hoặc dùng script Linux/macOS có sẵn:

```bash
export DB_PASSWORD="mat_khau_database"
chmod +x server/run-server.sh
./server/run-server.sh 5050 0.0.0.0
```

Server mặc định lắng nghe tại port `5050`. Nếu chạy client trên máy khác, thay `127.0.0.1` bằng IP public hoặc domain của máy server.

### Chạy server trên VPS riêng

#### 1. Đưa code mới lên VPS

Cách khuyến nghị là đẩy code từ máy local lên GitHub, sau đó SSH vào VPS để kéo bản mới nhất.

Trên máy local:

```bash
git status
git add .
git commit -m "cap nhat code"
git push origin main
```

Trên VPS:

```bash
ssh root@172.104.50.54
cd /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
git fetch origin main
git checkout main
git pull origin main
```

Nếu VPS chưa có source code, clone project lần đầu:

```bash
ssh root@172.104.50.54
cd /root
git clone https://github.com/PhamVietLong-25021855/Nhom14--BaiTapLon-Java.git Nhom14--BaiTapLon-Java-Long-BanGoc1
cd /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
```

Nếu không muốn dùng GitHub, có thể nén source ở máy local rồi copy trực tiếp lên VPS.

Windows PowerShell, chạy từ thư mục gốc project:

```powershell
git archive --format=zip -o auction-source.zip HEAD
scp .\auction-source.zip root@172.104.50.54:/root/auction-source.zip
```

Trên VPS:

```bash
mkdir -p /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
unzip -o /root/auction-source.zip -d /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
cd /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
```

#### 2. Build và chạy server trên VPS

SSH vào VPS:

```bash
ssh root@172.104.50.54
```

Vào thư mục project trên VPS:

```bash
cd /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
```

Dừng server cũ nếu đang chạy:

```bash
pkill -f server-1.0.0-SNAPSHOT.jar
```

Cấu hình mật khẩu database:

```bash
export DB_PASSWORD="mat_khau_database"
```

Build lại `core-common` và `server`:

```bash
mvn clean install -pl core-common,server -am
```

Chạy server nền và ghi log vào `log.txt`:

```bash
nohup java -jar server/target/server-1.0.0-SNAPSHOT.jar > log.txt 2>&1 &
```

Kiểm tra server đã lắng nghe port `5050`:

```bash
ss -tulnp | grep 5050
```

Xem log server:

```bash
tail -f log.txt
```

### 2. Chạy client sau khi server đã sẵn sàng

Cách 1: chạy bằng Maven, dùng được trên Windows/Linux/macOS.

Windows PowerShell:

```powershell
$env:APP_SERVER_HOST="127.0.0.1"
$env:APP_SERVER_PORT="5050"
mvn -ntp -pl client -am install -DskipTests
mvn -ntp -f client/pom.xml javafx:run
```

Linux/macOS:

```bash
export APP_SERVER_HOST="127.0.0.1"
export APP_SERVER_PORT="5050"
mvn -ntp -pl client -am install -DskipTests
mvn -ntp -f client/pom.xml javafx:run
```

Cách 2: chạy nhanh trên Windows bằng script có sẵn:

```powershell
cd client
.\run-client.ps1 -ServerHost "127.0.0.1" -ServerPort 5050
```

Nếu server nằm trên VPS:

```powershell
cd client
.\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

## Chức năng đã hoàn thành

- Đăng ký, đăng nhập, đổi mật khẩu và cập nhật hồ sơ người dùng.
- Phân quyền người dùng theo vai trò `ADMIN`, `SELLER`, `BIDDER`.
- Seller tạo, sửa, xóa/hủy và đóng phiên đấu giá.
- Bidder xem danh sách phiên đấu giá, xem chi tiết, đặt giá và xem lịch sử đặt giá.
- Tự động đặt giá theo mức tối đa do bidder cấu hình.
- Ví điện tử: nạp tiền, xem tổng số dư, số dư khả dụng và số tiền đang bị giữ.
- Giữ tiền khi bidder đang dẫn đầu, hoàn tiền khi bị vượt giá, trừ tiền khi phiên được xác nhận thanh toán.
- Admin quản lý tài khoản, thông báo trang chủ và lệnh đóng sớm phiên đấu giá.
- Hệ thống thông báo/inbox cho các sự kiện quan trọng.
- Server xử lý request qua socket, tách client khỏi truy cập database trực tiếp.
- Database initializer và script index hỗ trợ khởi tạo/tối ưu các bảng chính.
- Test JUnit cho các luồng quan trọng như validation, network request/response, cache, concurrent bidding, anti-sniping và settlement.

## Tài liệu, báo cáo và video demo

- Báo cáo PDF: [Cập nhật link báo cáo PDF](docs/BAO_CAO_NHOM_14.pdf)
- Video demo: [Cập nhật link video demo](https://example.com/video-demo)
- Hướng dẫn deploy: [docs/DEPLOY-GUIDE.md](docs/DEPLOY-GUIDE.md)
- Tài liệu tách client/server: [docs/split-client-server.md](docs/split-client-server.md)
- Tài liệu design patterns: [docs/design-patterns.md](docs/design-patterns.md)
- Tài liệu tối ưu: [docs/optimization-guide.md](docs/optimization-guide.md)

## Ghi chú lỗi thường gặp

| Lỗi | Cách kiểm tra |
| --- | --- |
| Client không kết nối được server | Kiểm tra IP/domain, port `5050`, firewall và log server |
| Server không kết nối được database | Kiểm tra `DB_PASSWORD`, host/port MySQL và quyền truy cập database |
| Không đặt giá được | Kiểm tra số dư khả dụng, giá hiện tại và quy tắc bước giá |
| JavaFX không chạy | Kiểm tra JDK 21, Maven và plugin JavaFX |
