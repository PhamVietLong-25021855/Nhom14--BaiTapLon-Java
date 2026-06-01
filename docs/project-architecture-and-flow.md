# Giải thích chi tiết kiến trúc, thư mục và luồng chạy của project

Tài liệu này giải thích tổng quan project đấu giá trực tuyến theo hướng dễ đọc: mỗi thư mục dùng để làm gì, các file liên hệ với nhau như thế nào, luồng chạy client-server-database-socket ra sao và các file `.jar` có vai trò gì.

## 1. Tổng quan kiến trúc

Project được tổ chức theo mô hình Maven multi-module, gồm 3 module chính:

| Module | Vai trò |
| --- | --- |
| `core-common` | Chứa code dùng chung giữa client và server: model, API, request/response socket, validation, util. |
| `client` | Ứng dụng JavaFX hiển thị giao diện và gửi request tới server qua socket. |
| `server` | Ứng dụng server nhận request socket, xử lý nghiệp vụ, truy cập database MySQL và trả response cho client. |

Luồng tổng quát:

```text
Người dùng
  -> Giao diện JavaFX ở client
  -> Remote service
  -> RemoteAuctionClient
  -> Socket TCP/TLS
  -> AuctionSocketServer
  -> AuctionRequestHandler
  -> Controller
  -> Service
  -> DAO
  -> MySQL database
```

Client không truy cập database trực tiếp. Mọi thao tác với MySQL chỉ nằm ở phía server. Đây là điểm quan trọng nhất của kiến trúc client-server trong project này.

## 2. Cấu trúc thư mục gốc

```text
.
|-- pom.xml
|-- core-common/
|-- client/
|-- server/
|-- docs/
|-- scripts/
|-- database_indexes.sql
|-- run-javafx.ps1
|-- deploy.sh
`-- test-autobid-stability.ps1
```

Ý nghĩa từng phần:

- `pom.xml`: file Maven cha, khai báo 3 module `core-common`, `client`, `server`, phiên bản Java, JUnit, MySQL driver và plugin build/test.
- `core-common/`: module dùng chung, được cả client và server phụ thuộc.
- `client/`: code giao diện JavaFX và lớp gọi server qua socket.
- `server/`: code server, service, DAO, database, socket server.
- `docs/`: tài liệu mô tả kiến trúc, triển khai, bảo mật socket, tối ưu và giải thích nâng cấp.
- `scripts/`: script hỗ trợ triển khai, restart server hoặc tách gói.
- `database_indexes.sql`: script thêm index cho database để tăng tốc truy vấn.
- `run-javafx.ps1`: script hỗ trợ chạy nhanh client JavaFX.
- `deploy.sh`: script hỗ trợ triển khai trên môi trường Linux/VPS.
- `test-autobid-stability.ps1`: script hỗ trợ kiểm tra ổn định logic tự động đặt giá.

## 3. Mối liên hệ giữa các module Maven

Quan hệ phụ thuộc:

```text
auction-house
|-- core-common
|-- client       -> phụ thuộc core-common
`-- server       -> phụ thuộc core-common
```

`core-common` đứng ở giữa để tránh lặp code. Những class mà cả client và server cần hiểu giống nhau sẽ đặt ở đây, ví dụ:

- `User`, `Auction`, `Bid`, `Wallet` trong package model.
- `AuctionRequest`, `AuctionResponse`, `NetworkActions` trong package network.
- Các interface API như `AuthApi`, `AuctionApi`.
- Validation và util dùng chung.

Nếu client và server không dùng chung cùng một phiên bản `core-common`, request/response có thể không khớp và gây lỗi khi chạy socket.

## 4. Module `core-common`

Đường dẫn chính:

```text
core-common/src/main/java/userauth
|-- api
|-- common
|-- controller
|-- event
|-- exception
|-- model
|-- network
|-- util
`-- validation
```

### `api`

Chứa các interface mô tả chức năng hệ thống, ví dụ:

- `AuthApi`: các chức năng đăng ký, đăng nhập, đổi mật khẩu, cập nhật tài khoản.
- `AuctionApi`: các chức năng tạo phiên đấu giá, sửa, xóa, đặt giá, lấy danh sách.

Mục đích của package này là tạo hợp đồng chung. Client gọi qua remote service, server hiện thực bằng controller/service.

### `controller`

Chứa các controller trung gian như `AuthController`, `AuctionController`, `WalletController`. Controller nhận dữ liệu đã được chuẩn bị, gọi service tương ứng và trả kết quả.

Trong server, `AuctionRequestHandler` sẽ gọi các controller này sau khi đọc request socket.

### `model`

Chứa các đối tượng dữ liệu chính của hệ thống, ví dụ:

- `User`: thông tin người dùng.
- `Auction`: phiên/sản phẩm đấu giá.
- `Bid`: lượt đặt giá.
- `Wallet`: ví người dùng.
- Các model phụ như thông báo, cấu hình tự động đặt giá.

Các model này phải nằm ở `core-common` vì cả client và server đều cần serialize/deserialize qua socket.

### `network`

Đây là phần quan trọng của giao tiếp socket:

- `AuctionRequest`: object client gửi lên server. Gồm `action`, `params`, `sessionToken`.
- `AuctionResponse`: object server trả về client. Gồm trạng thái thành công/thất bại, dữ liệu hoặc lỗi.
- `NetworkActions`: danh sách tên hành động như `AUTH_LOGIN`, `AUCTION_CREATE`, `AUCTION_PLACE_BID`.
- `AuthenticatedUserResponse`: response đăng nhập gồm user an toàn và session token.

Ví dụ một request đặt giá sẽ có dạng logic:

```text
action = AUCTION_PLACE_BID
params = { auctionId, amount, ... }
sessionToken = token sau đăng nhập
```

### `validation`, `util`, `event`, `exception`

- `validation`: kiểm tra dữ liệu đầu vào như email, mật khẩu, thông tin phiên đấu giá.
- `util`: tiện ích dùng chung, ví dụ xử lý mật khẩu hoặc format.
- `event`: cơ chế phát sự kiện nội bộ khi auction/bid thay đổi.
- `exception`: các loại lỗi nghiệp vụ dùng chung.

## 5. Module `client`

Đường dẫn chính:

```text
client/src/main/java/userauth
|-- ClientLauncher.java
|-- ClientMain.java
|-- gui/
`-- remote/
```

### `ClientLauncher.java`

Đây là điểm khởi chạy client JavaFX. Khi chạy client bằng Maven hoặc script, class này sẽ mở ứng dụng giao diện.

### `ClientMain.java`

Là class cấu hình/chạy ứng dụng JavaFX chính. Nó nạp giao diện, khởi tạo các thành phần UI và bắt đầu vòng đời client.

### `gui`

Chứa toàn bộ phần giao diện:

- FXML controller cho màn hình đăng nhập, đăng ký, trang chủ, seller, bidder, admin.
- Logic xử lý nút bấm, bảng dữ liệu, form nhập liệu.
- Điều hướng giữa các màn hình.
- Cập nhật UI sau khi nhận dữ liệu từ server.

Controller trong `gui` không nên truy cập database. Khi cần dữ liệu, nó gọi các service trong package `remote`.

### `remote`

Đây là lớp cầu nối giữa giao diện client và socket server:

```text
RemoteAuthService
RemoteAuctionService
RemoteWalletService
RemoteAutobidService
RemoteNotificationService
RemoteHomepageContentService
RemoteAuctionClient
RemoteClientConfig
```

Vai trò:

- `RemoteAuthService`: gửi request đăng nhập, đăng ký, logout, đổi mật khẩu.
- `RemoteAuctionService`: gửi request tạo auction, lấy danh sách, đặt giá, đóng phiên.
- `RemoteWalletService`: gửi request xem ví, nạp tiền, lịch sử nạp.
- `RemoteAutobidService`: gửi request tạo/sửa/xóa tự động đặt giá.
- `RemoteNotificationService`: gửi/lấy thông báo.
- `RemoteHomepageContentService`: xử lý nội dung trang chủ.
- `RemoteClientConfig`: đọc host, port, TLS của server từ biến môi trường/JVM property.
- `RemoteAuctionClient`: class socket dùng chung, tạo `AuctionRequest`, gửi qua socket và đọc `AuctionResponse`.

Luồng trong client:

```text
FXML Controller
  -> RemoteAuctionService / RemoteAuthService / ...
  -> RemoteAuctionClient.call(action, params)
  -> Gửi AuctionRequest qua socket
  -> Nhận AuctionResponse
  -> Trả dữ liệu về controller
  -> Cập nhật giao diện
```

## 6. Module `server`

Đường dẫn chính:

```text
server/src/main/java/userauth
|-- server/
|-- service/
|-- dao/
`-- database/
```

### Package `server`

Các file chính:

- `AuctionServerMain.java`
- `AuctionSocketServer.java`
- `AuctionRequestHandler.java`
- `AuctionSessionManager.java`
- `ServerContext.java`

Vai trò từng file:

- `AuctionServerMain.java`: điểm khởi chạy server. Đọc port, host bind, TLS, tạo `ServerContext`, tạo `AuctionSocketServer` và bắt đầu lắng nghe client.
- `AuctionSocketServer.java`: mở cổng socket, nhận kết nối, áp dụng timeout, thread pool, object filter và chuyển request cho handler.
- `AuctionRequestHandler.java`: đọc `action` trong `AuctionRequest`, kiểm tra session/quyền, gọi controller/service phù hợp và tạo `AuctionResponse`.
- `AuctionSessionManager.java`: quản lý session token sau đăng nhập, hết hạn session, giới hạn số session mỗi user và logout.
- `ServerContext.java`: khởi tạo toàn bộ dependency phía server như DAO, service, controller, scheduler.

Luồng khởi động server:

```text
AuctionServerMain.main()
  -> đọc cấu hình port/host/TLS
  -> new ServerContext(true)
  -> DatabaseInitializer.initialize()
  -> tạo DAO
  -> tạo Service
  -> tạo Controller
  -> khởi động AuctionScheduler
  -> new AuctionSocketServer(...)
  -> server.start()
```

### Package `service`

Chứa logic nghiệp vụ chính:

- `AuthService`: đăng ký, đăng nhập, quản lý tài khoản.
- `AuctionService`: tạo/sửa/xóa auction, đặt giá, đóng phiên, xử lý kết quả đấu giá.
- `WalletService`: xử lý ví, số dư, giữ tiền, hoàn tiền, nạp tiền.
- `AutobidService`: xử lý tự động đặt giá.
- `NotificationService`: tạo và lấy thông báo.
- `HomepageContentService`: xử lý nội dung trang chủ.
- `AuctionScheduler`: chạy nền để cập nhật trạng thái phiên đấu giá theo thời gian.
- `AuctionCache`: cache dữ liệu auction để giảm truy vấn database.
- `AuctionSettlementHandler` và `AuctionSettlementHandlerFactory`: xử lý quyết toán khi phiên đấu giá kết thúc.

Service là nơi chứa quy tắc nghiệp vụ. Ví dụ khi bidder đặt giá:

```text
Kiểm tra auction còn mở
  -> Kiểm tra giá mới hợp lệ
  -> Kiểm tra ví bidder đủ tiền
  -> Giữ tiền bidder mới
  -> Hoàn tiền bidder cũ nếu bị vượt giá
  -> Lưu bid
  -> Cập nhật auction
  -> Gửi thông báo nếu cần
```

### Package `dao`

DAO là lớp truy cập database:

- `UserDAO`, `UserDAOImpl`: đọc/ghi user.
- `AuctionDAO`, `AuctionDAOImpl`: đọc/ghi auction và bid.
- `WalletDAO`, `WalletDAOImpl`: đọc/ghi ví và giao dịch.
- `AutoBidDAO`, `AutoBidDAOImpl`: đọc/ghi cấu hình tự động đặt giá.
- `NotificationDAO`, `NotificationDAOImpl`: đọc/ghi thông báo.
- `HomepageAnnouncementDAO`, `HomepageAnnouncementDAOImpl`: đọc/ghi thông báo trang chủ.

Service không tự viết SQL trực tiếp nhiều nơi, mà gọi DAO để thao tác database. Cách này giúp tách nghiệp vụ khỏi hạ tầng lưu trữ.

### Package `database`

Chứa phần kết nối và khởi tạo database:

- Đọc cấu hình database từ `server/src/main/resources/database.properties`.
- Lấy mật khẩu từ `DB_PASSWORD` hoặc JVM property `-Ddb.password=...`.
- Tạo kết nối MySQL.
- Khởi tạo bảng/cấu trúc cần thiết khi server chạy.

Nếu sai mật khẩu database, server sẽ lỗi khi khởi tạo `ServerContext`, trước khi socket sẵn sàng nhận client.

## 7. Luồng socket chi tiết

Socket là kênh giao tiếp giữa client và server.

### Phía client

1. Người dùng thao tác trên giao diện.
2. FXML controller gọi remote service.
3. Remote service gọi `RemoteAuctionClient.call(...)`.
4. `RemoteAuctionClient` tạo `AuctionRequest`.
5. Client mở socket tới `APP_SERVER_HOST:APP_SERVER_PORT`.
6. Client ghi object request bằng `ObjectOutputStream`.
7. Client đọc object response bằng `ObjectInputStream`.
8. Nếu response thành công, dữ liệu được trả về UI.
9. Nếu response lỗi, client ném `RemoteServerException` hoặc hiển thị lỗi.

### Phía server

1. `AuctionSocketServer` lắng nghe port, mặc định `5050`.
2. Khi có client kết nối, server đưa kết nối vào thread pool xử lý.
3. Server đọc `AuctionRequest`.
4. `AuctionRequestHandler` kiểm tra `action`.
5. Nếu action cần đăng nhập, handler kiểm tra `sessionToken`.
6. Handler gọi controller/service tương ứng.
7. Service gọi DAO nếu cần database.
8. Server đóng gói kết quả vào `AuctionResponse`.
9. Server ghi response về client qua socket.

### Vì sao cần `NetworkActions`

Thay vì client gửi chuỗi tự do, toàn bộ action được gom vào `NetworkActions`. Điều này giúp:

- Tránh sai tên action.
- Dễ tìm nơi xử lý request.
- Dễ mở rộng chức năng mới.
- Client và server dùng chung một danh sách action.

## 8. Session và bảo mật khi truy cập qua socket

Sau khi đăng nhập thành công:

```text
Client gửi AUTH_LOGIN
  -> Server kiểm tra tài khoản/mật khẩu
  -> Server tạo session token
  -> Server trả AuthenticatedUserResponse
  -> Client lưu session token
```

Từ request tiếp theo:

```text
Client gửi request kèm sessionToken
  -> Server kiểm tra token trong AuctionSessionManager
  -> Server xác định user thật từ token
  -> Server xử lý nếu user có quyền
```

Lý do làm như vậy:

- Client không cần gửi lại mật khẩu sau khi đăng nhập.
- Server không tin `userId` do client tự truyền lên.
- Có thể logout bằng cách hủy session.
- Có thể chặn user bị khóa/xóa.
- Giảm nguy cơ lộ mật khẩu qua socket hoặc log.

Ngoài session, socket còn được tăng an toàn bằng:

- `ObjectInputFilter`: giới hạn class được phép deserialize.
- Read timeout: tránh client treo kết nối.
- Thread pool có giới hạn: tránh tạo vô hạn thread.
- TLS tùy chọn: mã hóa dữ liệu khi chạy qua mạng không tin cậy.

## 9. Luồng database

Database chỉ được truy cập từ server.

```text
Client
  -> Socket request
  -> Server
  -> Service
  -> DAO
  -> MySQL
```

Client không có:

- Mật khẩu database.
- DAO.
- File cấu hình database.
- MySQL driver để truy cập trực tiếp database.

Lợi ích:

- Bảo mật hơn vì mật khẩu DB chỉ nằm ở server.
- Dễ kiểm soát quyền truy cập.
- Dữ liệu được xử lý qua service nên đảm bảo đúng nghiệp vụ.
- Khi thay đổi database, client không cần sửa nhiều.

Các bảng chính thường liên quan:

- User/tài khoản.
- Auction/phiên đấu giá.
- Bid/lượt đặt giá.
- Wallet/ví và giao dịch.
- AutoBid/tự động đặt giá.
- Notification/thông báo.
- Homepage announcement/nội dung trang chủ.

## 10. Luồng chức năng chính

### Đăng nhập

```text
LoginView
  -> RemoteAuthService.login()
  -> RemoteAuctionClient.call(AUTH_LOGIN)
  -> AuctionSocketServer
  -> AuctionRequestHandler
  -> AuthController
  -> AuthService
  -> UserDAO
  -> MySQL
  -> AuthenticatedUserResponse
  -> Client lưu session token
```

### Tạo sản phẩm/phiên đấu giá

```text
Seller UI
  -> RemoteAuctionService.createAuction()
  -> AUCTION_CREATE + sessionToken
  -> AuctionRequestHandler kiểm tra seller
  -> AuctionController
  -> AuctionService
  -> AuctionDAO
  -> MySQL
  -> AuctionResponse
```

### Đặt giá sản phẩm

```text
Bidder UI
  -> RemoteAuctionService.placeBid()
  -> AUCTION_PLACE_BID + sessionToken
  -> Server kiểm tra bidder
  -> AuctionService kiểm tra phiên đấu giá
  -> WalletService kiểm tra và giữ tiền
  -> AuctionDAO lưu bid
  -> NotificationService gửi thông báo nếu cần
  -> Trả kết quả về client
```

### Tự động đặt giá

```text
Bidder cấu hình autobid
  -> RemoteAutobidService
  -> AUTOBID_CREATE / AUTOBID_UPDATE
  -> AutobidService
  -> AutoBidDAO
  -> Khi có bid mới, AuctionService kiểm tra autobid phù hợp
```

### Ví điện tử

```text
Wallet UI
  -> RemoteWalletService
  -> WALLET_GET / WALLET_TOP_UP
  -> WalletController
  -> WalletService
  -> WalletDAO
  -> MySQL
```

## 11. Các file `.jar` trong project

Sau khi chạy Maven package, mỗi module có thể sinh ra file `.jar` trong thư mục `target`.

### `core-common/target/core-common-1.0.0-SNAPSHOT.jar`

Đây là jar thư viện dùng chung.

Chứa:

- Model.
- API.
- Request/response socket.
- Validation.
- Util.

Vai trò:

- Client cần jar này để biết cấu trúc request/response và model.
- Server cần jar này để đọc đúng object client gửi lên.
- Jar này không phải ứng dụng chạy độc lập.

### `server/target/server-1.0.0-SNAPSHOT.jar`

Đây là jar chạy server.

Chứa:

- Code server.
- Code dùng chung từ `core-common`.
- MySQL driver.
- Main class: `userauth.server.AuctionServerMain`.

Vai trò:

- Dùng để chạy server trên máy local hoặc VPS.
- Mở port socket, mặc định `5050`.
- Kết nối database.
- Nhận request từ client và trả response.

Lệnh chạy thường dùng:

```bash
java -Dapp.server.port=5050 -Dapp.server.bind.host=0.0.0.0 -jar server/target/server-1.0.0-SNAPSHOT.jar
```

Nếu server cần database thật, phải có mật khẩu:

```bash
export DB_PASSWORD="mat_khau_database"
```

Hoặc trên PowerShell:

```powershell
$env:DB_PASSWORD="mat_khau_database"
```

### `client/target/client-1.0.0-SNAPSHOT.jar`

Đây là jar của module client.

Chứa:

- Code client.
- Remote service.
- Code dùng chung từ `core-common`.

Lưu ý:

- Client JavaFX thường được chạy bằng Maven JavaFX plugin hoặc script `client/run-client.ps1`.
- Jar client không nhất thiết là một file chạy độc lập hoàn chỉnh như server jar, vì JavaFX còn cần runtime/module path phù hợp.
- Khi chạy bằng script, script sẽ cấu hình host/port server và môi trường JavaFX thuận tiện hơn.

Lệnh chạy client thường dùng:

```powershell
cd client
.\run-client.ps1 -ServerHost "127.0.0.1" -ServerPort 5050
```

## 12. Các script quan trọng

### `server/run-server.ps1`

Dùng để chạy server trên Windows PowerShell. Script hỗ trợ:

- Truyền port.
- Truyền bind host.
- Bật TLS nếu cần.
- Truyền keystore nếu chạy TLS.
- Đặt đúng thư mục làm việc.

### `server/run-server.sh`

Dùng để chạy server trên Linux/VPS.

### `client/run-client.ps1`

Dùng để chạy client JavaFX trên Windows. Script hỗ trợ:

- Truyền host server.
- Truyền port server.
- Bật TLS nếu cần.
- Truyền truststore nếu chạy TLS.

### `scripts/restart-server.ps1`

Dùng để restart server theo cấu hình thống nhất với script chạy server chính.

## 13. Vì sao phải tách client, server và database

Nếu client truy cập database trực tiếp:

- Mật khẩu database phải nằm trên máy client.
- Người dùng có thể dò thông tin kết nối DB.
- Khó kiểm soát quyền truy cập.
- Mỗi thay đổi database có thể làm client hỏng.

Khi tách thành client-server:

- Client chỉ biết server host/port.
- Server giữ mật khẩu database.
- Server kiểm tra session và quyền.
- Business logic tập trung ở server.
- Dễ triển khai server trên VPS, client chạy ở nhiều máy khác nhau.

## 14. Tóm tắt mối liên hệ quan trọng

```text
README.md
  -> hướng dẫn tổng quan project

pom.xml
  -> quản lý module và dependency chung

core-common
  -> định nghĩa dữ liệu và giao thức chung

client
  -> giao diện + remote service + socket client

server
  -> socket server + nghiệp vụ + DAO + database

docs
  -> tài liệu giải thích, triển khai, bảo mật, tối ưu

scripts
  -> script hỗ trợ chạy/triển khai

database_indexes.sql
  -> tối ưu truy vấn database
```

Luồng chạy cuối cùng:

```text
Người dùng thao tác trên JavaFX
  -> Client tạo request
  -> Socket gửi request đến server
  -> Server kiểm tra session/quyền
  -> Service xử lý nghiệp vụ
  -> DAO thao tác MySQL
  -> Server trả response
  -> Client cập nhật giao diện
```

Đây là mối liên hệ cốt lõi cần nắm khi đọc hoặc bảo trì project.
