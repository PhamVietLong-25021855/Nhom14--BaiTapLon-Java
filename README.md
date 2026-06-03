# Nhóm 14 - Hệ thống đấu giá trực tuyến

Dự án xây dựng hệ thống đấu giá trực tuyến theo kiến trúc client-server. Client là ứng dụng JavaFX dùng để hiển thị giao diện và gửi request qua socket; server xử lý nghiệp vụ đấu giá, tài khoản, ví điện tử, thông báo và truy cập cơ sở dữ liệu MySQL.

Phạm vi hệ thống tập trung vào các luồng chính: đăng ký/đăng nhập, phân quyền người dùng, quản lý phiên đấu giá, đặt giá, tự động đặt giá, quản lý ví, thông báo và các thao tác quản trị cơ bản.

## Công nghệ sử dụng

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Công cụ đóng gói | Maven multi-module |
| Giao diện | JavaFX 21, FXML, CSS |
| Phía server | Java Socket Server |
| Cơ sở dữ liệu | MySQL / Akamai DB |
| Giao tiếp client-server | Object request/response qua socket |
| Kiểm thử | JUnit 5 |

## Môi trường và yêu cầu cài đặt

- JDK 21 trở lên.
- Maven 3.6.3 trở lên, hoặc dùng Maven Wrapper đi kèm project (`mvnw.cmd` / `mvnw`).
- MySQL hoặc Akamai DB có thể truy cập từ máy chạy server.
- Port TCP `5050` được mở nếu client kết nối đến server từ máy khác.
- Biến môi trường `DB_PASSWORD` hoặc JVM property `-Ddb.password=...` để server kết nối database.

Kiểm tra phiên bản:

```bash
java -version
./mvnw -version
```

Trên Windows có thể dùng:

```powershell
.\mvnw.cmd -version
```

## Cấu trúc thư mục

```text
.
|-- pom.xml                         Project Maven cha
|-- core-common/                    Mã dùng chung cho client và server
|   |-- src/main/java/userauth/api
|   |-- src/main/java/userauth/model
|   |-- src/main/java/userauth/network
|   |-- src/main/java/userauth/util
|   `-- src/main/java/userauth/validation
|-- server/                         Server socket phía xử lý nghiệp vụ
|   |-- src/main/java/userauth/server
|   |-- src/main/java/userauth/service
|   |-- src/main/java/userauth/dao
|   |-- src/main/java/userauth/database
|   `-- src/main/resources/database.properties
|-- client/                         Ứng dụng JavaFX
|   |-- src/main/java/userauth/remote
|   |-- src/main/java/userauth/gui
|   `-- src/main/resources
|-- docs/                           Tài liệu thiết kế, triển khai, tối ưu
|-- scripts/                        Script hỗ trợ triển khai và tách gói
`-- database_indexes.sql            Script bổ sung index database
```

## Cấu hình database

File cấu hình mặc định:

```text
server/src/main/resources/database.properties
```

Mật khẩu database không nên ghi trực tiếp vào mã nguồn. Khi chạy server, đặt biến môi trường:

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

## Đóng gói và kiểm thử

Chạy từ thư mục gốc của project.

Chạy toàn bộ test:

```bash
mvn -ntp test
```

Đóng gói toàn bộ project:

```bash
mvn -ntp package
```

Đóng gói server kèm các module phụ thuộc:

```bash
mvn -ntp -pl server -am package -DskipTests
```

Đóng gói client kèm các module phụ thuộc:

```bash
mvn -ntp -pl client -am package -DskipTests
```

## Thứ tự chạy server/client

Nếu server đã được chạy sẵn trên VPS của nhóm, người dùng trên Windows không cần chạy server cục bộ. Khi đó chỉ cần làm theo mục **2. Chạy client sau khi server đã sẵn sàng**.

Các bước chạy server bên dưới chỉ dành cho trường hợp cần chạy server local hoặc triển khai lại server trên VPS.

### 1. Chạy server trước

Đóng gói server:

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

Server mặc định lắng nghe tại port `5050`. Client của project đang dùng server public `172.104.50.54:5050`.

### Chạy server trên VPS riêng

#### 1. Đưa code mới lên VPS

Cách khuyến nghị là đẩy code từ máy cá nhân lên GitHub, sau đó SSH vào VPS để kéo bản mới nhất.

Trên máy cá nhân:

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

Nếu VPS chưa có mã nguồn, clone project lần đầu:

```bash
ssh root@172.104.50.54
cd /root
git clone https://github.com/PhamVietLong-25021855/Nhom14--BaiTapLon-Java.git Nhom14--BaiTapLon-Java-Long-BanGoc1
cd /root/Nhom14--BaiTapLon-Java-Long-BanGoc1
```

Nếu không muốn dùng GitHub, có thể nén mã nguồn ở máy cá nhân rồi sao chép trực tiếp lên VPS.

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

#### 2. Đóng gói và chạy server trên VPS

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

Đóng gói lại `core-common` và `server`:

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

#### Cách khuyến nghị trên Windows sau khi giải nén ZIP

Máy mới chỉ cần cài JDK 21 và có Internet trong lần chạy đầu tiên. Không cần cài Maven hoặc tải JavaFX SDK riêng.

Chạy từ thư mục gốc project:

```powershell
.\run-javafx.cmd
```

File này sẽ tự gọi PowerShell với `-ExecutionPolicy Bypass`, dùng Maven Wrapper `mvnw.cmd`, tải Maven 3.9.9 nếu chưa có, tải JavaFX 21 qua Maven, build lại client và chạy `userauth.ClientLauncher`.

Nếu server không dùng IP mặc định, truyền host/port:

```powershell
.\run-javafx.cmd -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

Khi đóng ZIP gửi sang máy khác, cần giữ nguyên các file/thư mục: `.mvn/`, `mvnw.cmd`, `mvnw`, `run-javafx.cmd`, `run-javafx.ps1`, `pom.xml`, `client/`, `core-common/`.

#### Chạy bằng Maven Wrapper

Dùng được trên Windows/Linux/macOS.

Windows PowerShell:

```powershell
$env:APP_SERVER_HOST="172.104.50.54"
$env:APP_SERVER_PORT="5050"
.\mvnw.cmd -ntp -f client/pom.xml clean javafx:run
```

Linux/macOS:

```bash
export APP_SERVER_HOST="172.104.50.54"
export APP_SERVER_PORT="5050"
./mvnw -ntp -f client/pom.xml clean javafx:run
```

Cách 2: chạy nhanh trên Windows bằng script có sẵn:

```powershell
.\run-javafx.cmd -ServerHost "172.104.50.54" -ServerPort 5050
```

Nếu server nằm trên VPS:

```powershell
.\run-javafx.cmd -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

## Chức năng đã hoàn thành

### Tài khoản và xác thực

- Người dùng có thể đăng ký tài khoản với vai trò `SELLER` hoặc `BIDDER`.
- Hệ thống kiểm tra username, email, mật khẩu và không cho đăng ký trùng username/email.
- Mật khẩu được hash bằng `PasswordUtil`, không lưu plaintext.
- Người dùng có thể đăng nhập, đăng xuất, đổi mật khẩu và cập nhật hồ sơ cá nhân.
- Tài khoản bị khóa sẽ không thể đăng nhập.
- Sau khi đăng nhập, server tạo session token để client dùng cho các request tiếp theo.

### Phân quyền người dùng

- Hệ thống có 3 vai trò chính: `ADMIN`, `SELLER`, `BIDDER`.
- `ADMIN` có quyền quản lý tài khoản, thông báo trang chủ và thao tác quản trị.
- `SELLER` có quyền tạo, sửa, xóa/hủy, đóng và xác nhận thanh toán phiên đấu giá của mình.
- `BIDDER` có quyền xem phiên đấu giá, đặt giá, cấu hình tự động đặt giá và quản lý ví.
- Server kiểm tra quyền bằng session token, không tin trực tiếp `userId` do client gửi lên.

### Chức năng dành cho Seller

- Tạo phiên đấu giá với tên sản phẩm, mô tả, giá khởi điểm, thời gian bắt đầu/kết thúc, danh mục, ảnh và bước giá.
- Cập nhật thông tin phiên đấu giá khi phiên chưa có bid và chưa ở trạng thái không cho sửa.
- Xóa phiên chưa có bid hoặc hủy phiên đã có bid theo đúng logic hoàn tiền/giải phóng tiền giữ.
- Xem danh sách các phiên đấu giá do seller tạo.
- Đóng phiên đấu giá thủ công khi có quyền.
- Đánh dấu phiên đã thanh toán (`PAID`) sau khi kết thúc.
- Hủy kết quả phiên đã kết thúc nếu cần xử lý hoàn tiền.

### Chức năng dành cho Bidder

- Xem danh sách phiên đấu giá và danh sách tóm tắt phiên đấu giá.
- Xem chi tiết từng phiên đấu giá.
- Xem lịch sử bid của một phiên.
- Đặt giá khi phiên đang chạy (`RUNNING`) và thời gian hiện tại nằm trong khoảng hợp lệ.
- Hệ thống kiểm tra giá đặt phải lớn hơn hoặc bằng `currentHighestBid + bidStep`.
- Bidder không được tự đặt giá tiếp nếu đang là người dẫn đầu.
- Khi đặt giá thành công, hệ thống cập nhật winner, giá cao nhất và lưu lịch sử bid.

### Tự động đặt giá

- Bidder có thể tạo rule tự động đặt giá theo `maxPrice` và `increment`.
- Nếu đã có rule cho cùng một auction và bidder, hệ thống cập nhật rule cũ thay vì tạo trùng.
- Bidder có thể sửa hoặc xóa rule autobid của chính mình.
- `increment` phải hợp lệ và không nhỏ hơn `bidStep` của phiên đấu giá.
- Khi có bid mới hoặc rule mới, hệ thống tự kích hoạt logic autobid nếu có bidder phù hợp.
- Hệ thống ưu tiên autobid theo mức `maxPrice` cao hơn, sau đó theo thời điểm tạo rule và id.
- Autobid vẫn kiểm tra số dư ví trước khi đặt giá tự động.

### Ví điện tử và thanh toán

- Mỗi user không phải admin có thể có ví điện tử.
- Bidder có thể xem tổng số dư, số tiền đang bị giữ và số dư khả dụng.
- Người dùng có thể tạo yêu cầu nạp tiền.
- Hiện luồng nạp tiền được tự động xác nhận thành công để phục vụ demo/chạy bài.
- Hệ thống lưu lịch sử nạp tiền trong `topup_transactions`.
- Hệ thống lưu log giao dịch ví trong `wallet_transactions`.
- Khi bidder đang dẫn đầu, số tiền tương ứng được giữ trong `reserved_balance`.
- Khi bidder bị vượt giá, tiền giữ được giải phóng.
- Khi phiên được xác nhận thanh toán, tiền giữ được capture/trừ khỏi ví.
- Khi phiên bị hủy, hệ thống release hoặc refund tiền theo trạng thái hiện tại.
- Có logic đối soát lại `reserved_balance` để giảm lỗi lệch số dư khi server khởi động lại.

### Luồng đấu giá và trạng thái phiên

- Phiên đấu giá có các trạng thái `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.
- Scheduler chạy nền để tự chuyển trạng thái theo thời gian.
- Khi đến `startTime`, phiên có thể chuyển từ `OPEN` sang `RUNNING`.
- Khi hết `endTime`, phiên chuyển sang `FINISHED` hoặc `PAID` tùy khả năng capture tiền.
- Có cơ chế anti-sniping: nếu có bid sát giờ kết thúc, hệ thống có thể kéo dài thời gian đóng phiên.
- Giới hạn số lần kéo dài anti-sniping để tránh phiên bị kéo dài vô hạn.
- Admin có thể kích hoạt early-close countdown cho phiên đang chạy.
- Nếu trong countdown có bid mới hoặc giá thay đổi, countdown được reset theo snapshot mới.

### Chức năng Admin

- Xem danh sách người dùng.
- Khóa hoặc mở khóa tài khoản người dùng.
- Xóa tài khoản người dùng khi cần quản trị.
- Xóa auction với quyền admin.
- Tạo, sửa, xóa thông báo/nội dung trang chủ.
- Tạo thông báo gửi đến người dùng.
- Kích hoạt hoặc hủy early-close countdown cho phiên đấu giá.
- Refresh trạng thái phiên đấu giá thủ công khi cần.

### Thông báo và nội dung trang chủ

- Hệ thống có notification/inbox cho người dùng.
- Admin có thể tạo thông báo.
- Người dùng có thể xem thông báo của chính mình.
- Người dùng có thể xóa một thông báo hoặc xóa toàn bộ thông báo của mình.
- Trang chủ có thể hiển thị các announcement do admin tạo.
- Announcement có thể chứa tiêu đề, tóm tắt, chi tiết, lịch trình và liên kết đến auction.

### Client JavaFX

- Client hiển thị giao diện bằng JavaFX, FXML và CSS.
- Client không truy cập database trực tiếp.
- Client gọi các remote service để gửi request qua socket.
- Client có thể chạy bằng `run-javafx.cmd` trên Windows.
- Script chạy client có thể tự dùng Maven Wrapper, tải Maven/JavaFX và build client trên máy mới.
- Client mặc định kết nối server `172.104.50.54:5050`, có thể đổi bằng `-ServerHost` và `-ServerPort`.

### Server socket

- Server chạy bằng `AuctionServerMain`.
- Server lắng nghe TCP port mặc định `5050`.
- Client gửi `AuctionRequest`, server trả `AuctionResponse`.
- Mỗi request được điều phối theo `NetworkActions`.
- Server kiểm tra session token và role trước khi xử lý action nhạy cảm.
- Server có thread pool để xử lý nhiều client.
- Server có timeout và `ObjectInputFilter` để giảm rủi ro deserialize object không mong muốn.
- Có tùy chọn bật TLS cho socket nếu cần triển khai trong môi trường không tin cậy.

### Database và lưu trữ

- Server là thành phần duy nhất truy cập MySQL/Akamai DB.
- Database config nằm trong `server/src/main/resources/database.properties`.
- Password database lấy từ `DB_PASSWORD` hoặc JVM property, không nên hardcode vào source.
- `DatabaseInitializer` tự tạo bảng nếu chưa có và đồng bộ một số column/index/constraint.
- Các bảng chính gồm `users`, `auctions`, `bids`, `auto_bids`, `wallets`, `topup_transactions`, `wallet_transactions`, `notifications`, `homepage_announcements`.
- Có script `database_indexes.sql` và các index trong initializer để tối ưu truy vấn quan trọng.

### Kiểm thử và ổn định

- Có test cho validation user, password util và serialize object qua network.
- Có test cho `AuctionRequest`, `AuctionResponse` và session manager.
- Có test cho concurrent bidding để kiểm tra nhiều bidder đặt giá cùng lúc.
- Có test cho anti-sniping, autobid, settlement và capture payment.
- Có test cho logic hoàn tiền khi xóa/hủy auction.
- Có test cho cache, status transition và reserved balance reconciliation.

## Tài liệu, báo cáo và video minh họa

- Báo cáo PDF: [Cập nhật link báo cáo PDF](https://drive.google.com/file/d/1C420oQgyEOYzfKRR8goD96f6edCBjPfj/view?usp=sharing)
- Video minh họa: [Cập nhật link video minh họa](https://drive.google.com/file/d/1Zz-7tz24TgetVcUgmFvQnDLasROE_3OZ/view?usp=sharing)
- Hướng dẫn chạy client cho người sử dụng: [docs/user-client-run-guide.md](docs/user-client-run-guide.md)
- Hướng dẫn triển khai/chạy server trên VPS: [docs/DEPLOY-GUIDE.md](docs/DEPLOY-GUIDE.md) và [server/README-SERVER.md](server/README-SERVER.md)
- Sơ đồ của Project: [Cập nhật link qua driver](https://drive.google.com/drive/folders/1GkMrqn5LPuqMnr9nf3yvCzFDxG_FbFGg?usp=sharing)

## Ghi chú lỗi thường gặp

| Lỗi | Cách kiểm tra |
| --- | --- |
| Client không kết nối được server | Kiểm tra IP/domain, port `5050`, firewall và log server |
| Server không kết nối được database | Kiểm tra `DB_PASSWORD`, host/port MySQL và quyền truy cập database |
| Không đặt giá được | Kiểm tra số dư khả dụng, giá hiện tại và quy tắc bước giá |
| JavaFX không chạy | Chạy `.\run-javafx.cmd`; kiểm tra JDK 21 và Internet trong lần chạy đầu |
| `local class incompatible` hoặc `serialVersionUID` | Client và server đang lệch phiên bản code; build/deploy lại server VPS cùng bản code với client |

Do server đã được chạy trên VPS riêng nên không cần chạy server cục bộ nữa; trên Windows chỉ cần chạy `.\run-javafx.cmd` từ thư mục gốc project.
