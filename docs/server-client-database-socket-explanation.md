# Giải Thích Client-Server, Database, Socket Và Folder Server

Tài liệu này giải thích cách project đấu giá trực tuyến chạy theo mô hình client-server. Trong project này, client JavaFX không truy cập database trực tiếp. Client chỉ tạo request và gửi qua socket đến server. Server mới là nơi kiểm tra đăng nhập, kiểm tra quyền, xử lý nghiệp vụ và đọc/ghi MySQL.

## 1. Kiến Trúc Tổng Quan

Project là Maven multi-module gồm 3 module chính:

| Module | Vai trò |
| --- | --- |
| `core-common` | Chứa code dùng chung cho cả client và server: model, API interface, request/response socket, action constants, exception, validation, util. |
| `client` | Ứng dụng JavaFX. Hiển thị UI và gọi server thông qua package `userauth.remote`. |
| `server` | Ứng dụng backend. Lắng nghe socket, xử lý request, gọi controller/service/DAO và truy cập database. |

Luồng tổng quát:

```text
Người dùng
  -> JavaFX UI trên client
  -> RemoteAuthService / RemoteAuctionService / ...
  -> RemoteAuctionClient
  -> Socket TCP hoặc TLS
  -> AuctionSocketServer
  -> AuctionRequestHandler
  -> Controller
  -> Service
  -> DAO
  -> MySQL database
  -> AuctionResponse trả về client
```

Điểm quan trọng:

- Client không có DAO.
- Client không đọc file `database.properties`.
- Client không biết mật khẩu database.
- Tất cả truy cập database nằm trong module `server`.
- `core-common` giúp client và server hiểu chung một kiểu object khi serialize qua socket.

## 2. Client-Server Là Gì Trong Project Này?

### Client

Client là ứng dụng JavaFX. Nhiệm vụ chính:

- Hiển thị màn hình đăng nhập, đăng ký, trang chủ, seller, bidder, admin.
- Nhận thao tác người dùng.
- Gọi các service remote như `RemoteAuthService`, `RemoteAuctionService`, `RemoteWalletService`.
- Đóng gói yêu cầu thành `AuctionRequest`.
- Gửi `AuctionRequest` qua socket đến server.
- Nhận `AuctionResponse` và cập nhật giao diện.

Ví dụ khi user đăng nhập:

```text
Login UI
  -> RemoteAuthService.login(username, password)
  -> RemoteAuctionClient.call(AUTH_LOGIN, "username", ..., "password", ...)
  -> Socket gửi AuctionRequest
```

### Server

Server là tiến trình riêng, chạy class `userauth.server.AuctionServerMain`. Nhiệm vụ chính:

- Mở cổng socket, mặc định `5050`.
- Nhận request từ nhiều client.
- Kiểm tra request có hợp lệ không.
- Kiểm tra session token và role.
- Gọi controller/service tương ứng.
- Service xử lý nghiệp vụ.
- DAO đọc/ghi database.
- Trả `AuctionResponse` về client.

Ví dụ server nhận request đăng nhập:

```text
AuctionSocketServer
  -> đọc AuctionRequest AUTH_LOGIN
  -> AuctionRequestHandler.dispatch()
  -> AuthController.login()
  -> AuthService.login()
  -> UserDAO.findByUsername()
  -> MySQL
  -> tạo session token
  -> trả AuthenticatedUserResponse
```

## 3. Socket Hoạt Động Như Thế Nào?

Project dùng Java socket với `ObjectOutputStream` và `ObjectInputStream`.

### Object client gửi đi

Client gửi object `AuctionRequest` trong `core-common`:

```java
new AuctionRequest(action, params, sessionToken)
```

`AuctionRequest` gồm:

| Thành phần | Ý nghĩa |
| --- | --- |
| `action` | Tên hành động, ví dụ `AUTH_LOGIN`, `AUCTION_PLACE_BID`, `WALLET_GET`. |
| `params` | Map chứa tham số, ví dụ `username`, `password`, `auctionId`, `amount`. |
| `sessionToken` | Token sau khi đăng nhập. Request nào cần đăng nhập sẽ gửi kèm token này. |

### Object server trả về

Server trả object `AuctionResponse`:

| Thành phần | Ý nghĩa |
| --- | --- |
| `success` | `true` nếu thành công, `false` nếu lỗi. |
| `data` | Dữ liệu trả về khi thành công. |
| `errorType` | Tên class lỗi, ví dụ `UnauthorizedException`, `ValidationException`. |
| `errorMessage` | Nội dung lỗi để client hiển thị/xử lý. |

### Mỗi kết nối socket xử lý một request

Trong `AuctionSocketServer`, mỗi lần client gọi:

```text
Client mở socket
  -> gửi 1 AuctionRequest
  -> server xử lý
  -> server trả 1 AuctionResponse
  -> đóng socket
```

Đây là mô hình request-response đơn giản. Nó không phải socket realtime giữ kết nối liên tục.

### Bảo vệ socket

Server có một số lớp bảo vệ:

- Giới hạn thread xử lý client bằng thread pool.
- Timeout đọc request là `10_000 ms`.
- Giới hạn kích thước request.
- Dùng `ObjectInputFilter` để chỉ cho deserialize các class được phép.
- Có tùy chọn TLS nếu bật `app.server.tls.enabled` hoặc `APP_SERVER_TLS_ENABLED`.

Client cũng có `ObjectInputFilter` khi đọc response, chỉ cho nhận các class trong `userauth.network`, `userauth.model`, `java.util` và một số kiểu cơ bản.

## 4. Session Và Phân Quyền

Sau khi đăng nhập thành công:

```text
Client gửi AUTH_LOGIN
  -> Server kiểm tra username/password
  -> Server tạo session token
  -> Server trả AuthenticatedUserResponse(user, token, expiresAt)
  -> Client lưu token trong RemoteAuctionClient
```

Từ request tiếp theo:

```text
Client gửi AuctionRequest(action, params, sessionToken)
  -> AuctionRequestHandler.requireAuthenticated()
  -> AuctionSessionManager.require(token)
  -> Lấy ra Session(userId, username, role, expiresAt)
```

Server không tin `userId` từ client cho các hành động nhạy cảm. Ví dụ:

- Đặt giá: server yêu cầu role `BIDDER`, sau đó dùng `principal.userId()` từ session.
- Tạo/sửa/xóa auction: server yêu cầu role `SELLER`, dùng `principal.userId()` làm seller.
- Quản lý user/homepage: server yêu cầu role `ADMIN`.
- Xem wallet/thông báo: server yêu cầu user chỉ được xem tài khoản của chính mình.

`AuctionSessionManager`:

- Tạo token ngẫu nhiên 32 bytes.
- Token hết hạn sau 8 giờ.
- Mỗi user tối đa 5 session đang hoạt động.
- Logout thì xóa token.
- Khi admin khóa/xóa user, server invalidate session của user đó.

## 5. Database Hoạt Động Như Thế Nào?

Database chỉ được truy cập từ server.

```text
Client
  -> Socket
  -> Server
  -> Service
  -> DAO
  -> DatabaseConnection
  -> MySQL
```

File cấu hình database nằm tại:

```text
server/src/main/resources/database.properties
```

Nó khai báo:

- `db.type=mysql`
- `db.host=...`
- `db.port=26281`
- `db.name=defaultdb`
- `db.username=akmadmin`
- `db.password=${DB_PASSWORD}`
- `db.sslMode=REQUIRED`

Password không nên commit lên Git. Khi chạy server, truyền password bằng biến môi trường:

```powershell
$env:DB_PASSWORD="mat_khau_database"
```

Hoặc JVM property:

```text
-Ddb.password=mat_khau_database
```

### Các bảng chính

| Bảng | Vai trò |
| --- | --- |
| `users` | Lưu tài khoản, mật khẩu đã hash, họ tên, email, role, status. |
| `auctions` | Lưu phiên/sản phẩm đấu giá, giá khởi điểm, giá hiện tại, seller, winner, status, thời gian. |
| `bids` | Lưu lịch sử đặt giá của từng auction. |
| `auto_bids` | Lưu cấu hình tự động đặt giá của bidder. |
| `wallets` | Lưu số dư và số tiền đang bị giữ của user. |
| `topup_transactions` | Lưu lịch sử nạp tiền. |
| `wallet_transactions` | Lưu log giao dịch ví: nạp, giữ tiền, release, capture, refund. |
| `notifications` | Lưu thông báo gửi đến user. |
| `homepage_announcements` | Lưu nội dung/thông báo hiện trên trang chủ. |

### Connection pool

`DatabaseConnection` có pool kết nối đơn giản:

- Tối đa từ 2 đến 6 connection tùy CPU.
- Khi DAO gọi `openDatabaseConnection()`, lấy connection idle nếu có.
- Khi `close()`, connection không đóng vật lý ngay mà được đưa lại pool.
- Nếu connection lỗi thật sự thì bị loại khỏi pool.

## 6. Luồng Khởi Động Server

Khi chạy server:

```text
AuctionServerMain.main()
  -> đọc port từ app.server.port hoặc APP_SERVER_PORT
  -> đọc bind host từ app.server.bind.host hoặc APP_SERVER_BIND_HOST
  -> đọc TLS từ app.server.tls.enabled hoặc APP_SERVER_TLS_ENABLED
  -> new ServerContext(true)
  -> DatabaseInitializer.initialize()
  -> tạo DAO
  -> tạo Service
  -> tạo Controller
  -> reconcileReservedBalances()
  -> start AuctionScheduler
  -> new AuctionSocketServer(...)
  -> server.start()
```

`ServerContext(true)` là nơi lắp ráp dependency:

```text
UserDAO -> AuthService -> AuthController
WalletDAO -> WalletService -> WalletController
AuctionDAO + AutoBidDAO + WalletService + NotificationService -> AuctionService -> AuctionController
AutoBidDAO + AuctionService -> AutobidService -> AutobidController
NotificationDAO -> NotificationService -> NotificationController
HomepageContentService -> HomepageController
AuctionScheduler -> chạy nền mỗi 1 giây
```

## 7. Luồng Request Chi Tiết

### Đăng ký

```text
Client:
RemoteAuthService.register()
  -> AUTH_REGISTER

Server:
AuctionRequestHandler
  -> AuthController.registerGUI()
  -> AuthService.register()
  -> validate username/password/email/role
  -> hash password
  -> UserDAO.save()
  -> WalletService.getWallet() nếu role không phải ADMIN
  -> trả "SUCCESS"
```

### Đăng nhập

```text
Client:
RemoteAuthService.login()
  -> AUTH_LOGIN

Server:
AuctionRequestHandler
  -> AuthController.login()
  -> AuthService.login()
  -> UserDAO.findByUsername()
  -> check password
  -> check user không bị BLOCKED
  -> AuctionSessionManager.create(user)
  -> trả AuthenticatedUserResponse
```

### Tạo auction

```text
Client:
RemoteAuctionService.createAuction()
  -> AUCTION_CREATE + sessionToken

Server:
AuctionRequestHandler.requireRole(SELLER)
  -> AuctionController.createAuction()
  -> AuctionService.createAuction()
  -> validate tên, giá, thời gian, image, bid step
  -> AuctionDAO.saveAuction()
  -> NotificationService.createNotification()
  -> trả "SUCCESS"
```

### Đặt giá

```text
Client:
RemoteAuctionService.placeBid()
  -> AUCTION_PLACE_BID + sessionToken

Server:
AuctionRequestHandler.requireRole(BIDDER)
  -> AuctionService.placeBid()
  -> lock theo auctionId
  -> load AuctionItem từ DB
  -> refresh status nếu cần
  -> check auction RUNNING
  -> check thời gian hiện tại trong start/end
  -> check bidder không phải winner hiện tại
  -> check amount >= currentHighestBid + bidStep
  -> check ví có đủ available balance
  -> tạo BidTransaction mới
  -> applyAutoBids()
  -> applyAntiSniping() nếu gần hết giờ
  -> applyReservationTransition()
  -> save các bid vào DB
  -> update auction state
  -> publish event
  -> trả "SUCCESS"
```

### Giữ tiền khi đặt giá

Logic tiền nằm trong `WalletService`.

Ví dụ winner cũ là A với 100.000, bidder mới B đặt 120.000:

```text
WalletService.applyReservationTransition(A, 100000, B, 120000)
  -> release reserved 100000 của A
  -> reserve 120000 của B
```

Nếu cùng một user tăng bid từ 100.000 lên 150.000:

```text
previousUserId == nextUserId
  -> chỉ reserve thêm 50.000
```

Khi auction kết thúc và thanh toán:

```text
captureReservedFunds(winnerId, amount)
  -> reserved_balance giảm
  -> balance giảm
  -> log WalletTransaction CAPTURE
```

Khi auction bị hủy:

```text
releaseReservedFunds(winnerId, amount)
  -> reserved_balance giảm
  -> balance không giảm
```

Nếu auction đã PAID rồi mới bị hủy:

```text
refundCapturedFunds(winnerId, amount)
  -> balance tăng lại
  -> log WalletTransaction REFUND
```

### Autobid

Autobid là cấu hình đặt giá tự động:

```text
bidderId
auctionId
maxPrice
increment
```

Khi tạo/sửa autobid:

```text
AutobidService
  -> validate maxPrice, increment
  -> increment phải >= bidStep của auction
  -> save/update auto_bids
  -> AuctionService.triggerAutoBids(auctionId)
```

Khi có bid mới, `AuctionService.applyAutoBids()`:

- Lấy danh sách auto bid theo auction.
- Bỏ qua current winner.
- Chỉ chọn auto bid có `maxPrice > currentHighestBid`.
- Kiểm tra bidder có đủ tiền trong wallet.
- Sắp xếp ưu tiên `maxPrice` cao hơn, sau đó `createdAt`, sau đó `id`.
- Tạo các bid tự động liên tiếp cho đến khi không còn bidder hợp lệ.

### Anti-sniping

Anti-sniping dùng để tránh tình huống đặt giá sát giờ kết thúc.

Nếu có bid mới khi thời gian còn lại nằm trong window anti-sniping:

```text
applyAntiSniping()
  -> kéo dài endTime
  -> tăng antiSnipingExtensionCount
  -> giới hạn số lần kéo dài bằng AuctionRules.MAX_ANTI_SNIPING_EXTENSIONS
```

### Scheduler

`AuctionScheduler` chạy nền mỗi 1 giây:

```text
while running:
  auctionService.refreshAuctionStatuses()
  sleep(1000)
```

Nhiệm vụ:

- Chuyển auction `OPEN` sang `RUNNING` khi đến `startTime`.
- Chuyển auction `RUNNING` sang `FINISHED` hoặc `PAID` khi hết giờ.
- Kích hoạt autobid khi auction bắt đầu/chạy.
- Xử lý admin early-close countdown.
- Gửi notification khi auction đóng.

## 8. Chức Năng Từng File Trong Folder `server`

### File ở gốc folder `server`

| File | Chức năng |
| --- | --- |
| `server/pom.xml` | Cấu hình Maven cho module server. Phụ thuộc `core-common`, MySQL connector, JavaFX graphics. Cấu hình `exec-maven-plugin` và `maven-shade-plugin` để tạo jar chạy class `userauth.server.AuctionServerMain`. |
| `server/README-SERVER.md` | Hướng dẫn build/chạy server, cổng mặc định và cách truyền password database. |
| `server/run-server.ps1` | Script chạy server trên Windows PowerShell. |
| `server/run-server.sh` | Script chạy server trên Linux/macOS/VPS. |
| `server/server.iml` | File cấu hình IntelliJ IDEA cho module server. Không phải code logic. |
| `server/target/` | Thư mục output sau khi build/test: `.class`, `.jar`, surefire reports. Không nên sửa tay. |

### Package `userauth.server`

| File | Chức năng |
| --- | --- |
| `AuctionServerMain.java` | Entry point của server. Đọc port, bind host, TLS; tạo `ServerContext`; tạo `AuctionSocketServer`; đăng ký shutdown hook; gọi `server.start()`. |
| `AuctionSocketServer.java` | Mở socket server. Accept client. Đưa mỗi client vào thread pool. Đọc `AuctionRequest`, gọi `AuctionRequestHandler`, ghi `AuctionResponse`. Có timeout, object filter, giới hạn request size, TLS optional. |
| `AuctionRequestHandler.java` | Dispatcher chính của server. Đọc `action`, switch theo `NetworkActions`, kiểm tra session/role, lấy param, gọi controller/service phù hợp, trả data. Lỗi được đóng gói thành `AuctionResponse.fail`. |
| `AuctionSessionManager.java` | Quản lý token đăng nhập: tạo token, check token, hết hạn sau 8 giờ, giới hạn 5 session/user, logout, invalidate user. |
| `ServerContext.java` | Lắp ráp dependency server: initialize DB, tạo DAO, tạo service, tạo controller, khởi động scheduler. Đây là nơi đảm bảo chỉ server mới tạo DAO và truy cập database. |

### Package `userauth.database`

| File | Chức năng |
| --- | --- |
| `DatabaseConfig.java` | Đọc cấu hình DB từ JVM property, env var, và `database.properties`. Tạo JDBC URL cho MySQL/PostgreSQL, xử lý SSL mode, host, port, username, password. |
| `DatabaseConnection.java` | Quản lý JDBC driver và pool connection đơn giản. Cung cấp `openDatabaseConnection()` cho DAO và `openServerConnection()` khi cần tạo database. |
| `DatabaseInitializer.java` | Khi server start, tạo bảng nếu chưa có, đồng bộ column/index/constraint, drop trigger cũ, test kết nối và in log kết nối thành công. Hỗ trợ cả MySQL và PostgreSQL. |
| `database.properties` | File resource cấu hình database mặc định. Password lấy từ `${DB_PASSWORD}`. |

### Package `userauth.dao`

DAO là lớp đọc/ghi database. Interface định nghĩa hàm, `DAOImpl` chứa SQL thật.

| File | Chức năng |
| --- | --- |
| `UserDAO.java` | Interface thao tác user: save, update, delete, find by id/username/email, find all. |
| `UserDAOImpl.java` | SQL cho bảng `users`. Khi xóa user, có xử lý các dữ liệu liên quan như bids, auto bids, homepage announcements, wallet, topup, wallet transactions, auctions; đồng thời tính lại auction nếu bid bị xóa. |
| `AuctionDAO.java` | Interface thao tác auction và bid: save/update/delete auction, lấy danh sách, lấy candidate refresh status, save bid, lấy bid count. |
| `AuctionDAOImpl.java` | SQL cho bảng `auctions` và `bids`. Map `ResultSet` thành `AuctionItem`/`BidTransaction`. Hỗ trợ summary list, find by seller, finished auctions, auctions holding reserved funds. |
| `AutoBidDAO.java` | Interface thao tác cấu hình autobid. |
| `AutoBidDAOImpl.java` | SQL cho bảng `auto_bids`: save/update/delete, find by id, find by auction+bidder, find by auction, find all autobid của user. |
| `WalletDAO.java` | Interface thao tác wallet, topup transaction và wallet transaction. |
| `WalletDAOImpl.java` | SQL cho `wallets`, `topup_transactions`, `wallet_transactions`. Lưu số dư, reserved balance, lịch sử nạp tiền và log giao dịch ví. |
| `NotificationDAO.java` | Interface thao tác notification. |
| `NotificationDAOImpl.java` | SQL cho bảng `notifications`: tạo notification, lấy theo user, xóa một notification, xóa tất cả notification của user. |
| `HomepageAnnouncementDAO.java` | Interface thao tác announcement trang chủ. |
| `HomepageAnnouncementDAOImpl.java` | SQL cho bảng `homepage_announcements`: save/update/delete/find/list. |

### Package `userauth.service`

Service là nơi xử lý business logic. DAO chỉ đọc/ghi DB, còn service quyết định dữ liệu có hợp lệ hay không.

| File | Chức năng |
| --- | --- |
| `AuthService.java` | Đăng ký, đăng nhập, đổi mật khẩu, update profile, khóa/mở khóa user, xóa user. Validate input, hash password, check account blocked, tạo wallet cho user mới. |
| `AuctionService.java` | Logic đấu giá chính: tạo/sửa/xóa auction, đặt giá, autobid, anti-sniping, đóng phiên, admin early-close, mark paid/cancel, refresh status, giữ/hoàn/capture tiền, publish event. Có lock riêng theo `auctionId` để tránh race condition khi nhiều người đặt giá cùng lúc. |
| `WalletService.java` | Quản lý ví: tạo wallet, nạp tiền, lịch sử nạp, check số dư khả dụng, reserve/release/capture/refund tiền, reconcile reserved balance. Có lock theo `userId` và lock theo thứ tự để tránh deadlock. |
| `AutobidService.java` | Tạo/sửa/xóa autobid. Validate `maxPrice`, `increment`, `increment >= bidStep`; sau khi thay đổi rule thì gọi `AuctionService.triggerAutoBids()`. |
| `NotificationService.java` | Tạo, lấy và xóa notification. Validate user id và gọi `NotificationDAO`. |
| `HomepageContentService.java` | Quản lý nội dung trang chủ. Admin có thể thêm/sửa/xóa announcement; service validate title, summary, schedule. |
| `AuctionScheduler.java` | Thread nền mỗi 1 giây gọi `AuctionService.refreshAuctionStatuses()`. |
| `AuctionCache.java` | Cache generic có TTL cho dữ liệu auction. Hiện tại là utility cache, dùng để giảm query nếu được tích hợp vào service/repository. |
| `AuctionSettlementHandler.java` | Interface nội bộ cho cách quyết toán auction khi chuyển status sang `PAID` hoặc `CANCELED`. |
| `AuctionSettlementHandlerFactory.java` | Factory tạo handler cho settlement: `PaidSettlementHandler` và `CanceledSettlementHandler`. |

### Package `server/src/test`

| File/nhóm test | Chức năng |
| --- | --- |
| `AuctionSessionManagerTest.java` | Test session token: tạo, hết hạn, invalidate, giới hạn session. |
| `AuctionSocketServerFilterTest.java` | Test object filter của socket server. |
| `AuctionSocketServerLoadTest.java` | Test khả năng xử lý nhiều kết nối client. |
| `AuctionAntiSnipingTest.java` | Test logic kéo dài thời gian khi bid sát giờ kết thúc. |
| `AuctionAutoPaymentCaptureTest.java` | Test capture tiền khi auction kết thúc/thanh toán. |
| `AuctionBidderSellerAutobidFlowTest.java` | Test luồng seller/bidder/autobid. |
| `AuctionCacheTest.java` | Test cache TTL và invalidate. |
| `AuctionConcurrentBiddingTest.java` | Test nhiều bidder đặt giá đồng thời. |
| `AuctionDeletionRefundTest.java` | Test hủy/xóa auction và hoàn/release tiền. |
| `AuctionPerformanceSmokeTest.java` | Test hiệu năng cơ bản. |
| `AuctionReservedBalanceReconciliationTest.java` | Test cân đối lại reserved balance. |
| `AuctionSettlementHandlerFactoryTest.java` | Test handler quyết toán PAID/CANCELED. |
| `AuctionStatusTransitionTest.java` | Test chuyển trạng thái OPEN/RUNNING/FINISHED/PAID/CANCELED. |
| `ServiceTestSupport.java` | Utility dùng chung cho test service. |

## 9. Vai Trò Của `core-common` Trong Giao Tiếp

`core-common` nằm giữa client và server.

Client cần nó để:

- Tạo `AuctionRequest`.
- Đọc `AuctionResponse`.
- Dùng model `User`, `AuctionItem`, `Wallet`, `AutoBid`, `Notification`.
- Dùng constants trong `NetworkActions`.
- Dùng interface API để remote service implement.

Server cần nó để:

- Deserialize đúng object client gửi.
- Trả về đúng model client hiểu.
- Dùng chung exception/model/rule.

Nếu client và server dùng hai version `core-common` khác nhau, có thể lỗi serialize/deserialize hoặc action không khớp.

### 9.1. Vai trò tổng thể của `core-common`

`core-common` là module dùng chung giữa `client` và `server`. Đây là phần định nghĩa dữ liệu, hợp đồng API, object truyền qua socket, exception, rule và utility mà cả hai bên đều phải hiểu giống nhau.

Nói ngắn gọn:

```text
core-common
  -> định nghĩa "client và server nói chuyện với nhau bằng cái gì"
  -> định nghĩa "dữ liệu trả qua lại có hình dạng thế nào"
  -> định nghĩa "service phải có những hàm nào"
  -> định nghĩa "lỗi nghiệp vụ dùng class nào"
```

Mối liên hệ Maven:

```text
pom.xml cha
|-- core-common
|-- client       -> phụ thuộc core-common
`-- server       -> phụ thuộc core-common
```

Vì `client` và `server` đều phụ thuộc `core-common`, các class như `User`, `AuctionItem`, `AuctionRequest`, `AuctionResponse`, `NetworkActions` được dùng chung. Nếu sửa các file này, cần build lại cả client và server.

Điểm cần nhớ: `core-common` không truy cập database và không mở socket server. Nó chỉ chứa code chung để hai bên hiểu cùng một giao thức và cùng một kiểu dữ liệu.

### 9.2. Cấu trúc folder `core-common`

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

Ý nghĩa từng package:

| Package | Vai trò |
| --- | --- |
| `api` | Chứa interface mô tả các chức năng hệ thống. Server implement bằng service thật, client implement bằng remote service gửi socket. |
| `common` | Chứa rule/hằng số nghiệp vụ dùng chung, ví dụ rule đấu giá. |
| `controller` | Lớp trung gian gọi API service, chuyển exception thành kết quả dạng `"SUCCESS"` hoặc message lỗi. |
| `event` | Cơ chế event nội bộ cho thay đổi auction: bid activity, status changed, settled, anti-sniping. |
| `exception` | Các exception nghiệp vụ dùng chung giữa client và server. |
| `model` | Các object dữ liệu chính: user, auction, bid, wallet, notification, autobid. |
| `network` | Object giao tiếp socket: request, response, action constants, response đăng nhập. |
| `util` | Tiện ích dùng chung: hash password, nén/giải nén ảnh. |
| `validation` | Validate input dùng chung, hiện có validate username/password/email. |

Luồng liên hệ tổng quát:

```text
client/gui
  -> client/remote/*Service
  -> core-common/api interface
  -> core-common/network/AuctionRequest
  -> server/AuctionRequestHandler
  -> core-common/controller
  -> server/service implements core-common/api
  -> server/dao
  -> database
```

### 9.3. Package `api`: hợp đồng chức năng

Package `api` chứa các interface. Interface là hợp đồng: nó quy định service phải có hàm nào, tham số gì, trả về gì, ném lỗi gì. Interface không xử lý logic thật.

Mối liên hệ quan trọng:

```text
core-common/api/AuthApi
  -> server/service/AuthService implements AuthApi
  -> client/remote/RemoteAuthService implements AuthApi

core-common/api/AuctionApi
  -> server/service/AuctionService implements AuctionApi
  -> client/remote/RemoteAuctionService implements AuctionApi
```

Nghĩa là cùng một interface nhưng có hai cách triển khai:

- Trên server: service xử lý thật và gọi DAO/database.
- Trên client: remote service chỉ đóng gói request rồi gửi socket.

| File | Chức năng |
| --- | --- |
| `AuctionApi.java` | Hợp đồng cho chức năng đấu giá: tạo/sửa/xóa auction, lấy auction, lấy danh sách, lấy bids, đếm bids, đặt giá, đóng phiên, admin early-close, mark paid, cancel finished auction, refresh status. |
| `AuthApi.java` | Hợp đồng xác thực và quản lý tài khoản: đăng ký, đăng nhập, logout, lấy user, lấy tất cả user, đổi mật khẩu, cập nhật profile, khóa/mở khóa user, xóa tài khoản. |
| `AutobidApi.java` | Hợp đồng cho autobid: tạo/sửa/xóa rule tự động đặt giá, lấy rule theo bidder, lấy rule theo id. |
| `HomepageContentApi.java` | Hợp đồng quản lý nội dung trang chủ: lấy tất cả announcement, lưu announcement, xóa announcement. |
| `NotificationApi.java` | Hợp đồng thông báo: tạo notification, lấy notification theo user, xóa một notification, xóa toàn bộ notification của user. |
| `WalletApi.java` | Hợp đồng ví: lấy wallet, tạo yêu cầu nạp tiền, lấy lịch sử nạp tiền. |

Ví dụ mối liên hệ của `AuctionApi.placeBid()`:

```text
Client UI
  -> RemoteAuctionService.placeBid()
  -> gửi NetworkActions.AUCTION_PLACE_BID qua socket

Server
  -> AuctionRequestHandler nhận AUCTION_PLACE_BID
  -> AuctionController.placeBid()
  -> AuctionService.placeBid()
  -> AuctionDAO + WalletService + Database
```

### 9.4. Package `controller`: lớp trung gian gọi API

Package `controller` chứa các controller nhận API interface qua constructor rồi gọi các hàm nghiệp vụ.

Ví dụ:

```java
public AuctionController(AuctionApi auctionService) {
    this.auctionService = auctionService;
}
```

Vì nhận `AuctionApi`, controller không bị phụ thuộc trực tiếp vào `AuctionService`. Nó có thể dùng với bất kỳ class nào implement `AuctionApi`.

Trong server, `ServerContext` tạo controller như sau:

```text
AuctionService -> AuctionController
AuthService -> AuthController
WalletService -> WalletController
...
```

Sau đó `AuctionRequestHandler` gọi controller theo action socket.

| File | Chức năng |
| --- | --- |
| `AuctionController.java` | Controller cho đấu giá. Gọi `AuctionApi` để tạo/sửa/xóa auction, lấy danh sách, đặt giá, đóng phiên, mark paid, cancel finished auction, refresh status. Nhiều hàm bắt exception và trả `"SUCCESS"` hoặc message lỗi. |
| `AuthController.java` | Controller cho tài khoản. Gọi `AuthApi` để đăng ký, đăng nhập, logout, lấy user, đổi mật khẩu, update profile, khóa/mở khóa user, xóa user. Có chặn đăng ký tài khoản `ADMIN` từ màn hình đăng ký. |
| `AutobidController.java` | Controller cho auto-bid. Gọi `AutobidApi` để tạo/sửa/xóa/lấy rule autobid. Chuyển lỗi thành chuỗi message để response dễ xử lý. |
| `HomepageController.java` | Controller cho nội dung trang chủ. Gọi `HomepageContentApi` để lấy, lưu, xóa announcement. Khi lưu dùng `currentUser.getId()` làm `authorId`. |
| `NotificationController.java` | Controller cho thông báo. Gọi `NotificationApi` để tạo, lấy, xóa một hoặc xóa tất cả notification. |
| `WalletController.java` | Controller cho ví. Gọi `WalletApi` để lấy wallet, tạo top-up request, lấy lịch sử top-up. Nếu không tìm thấy wallet có thể trả `null` hoặc list rỗng. |

Điểm cần lưu ý: controller trong `core-common` không tự gọi database. Nó chỉ gọi API/service được truyền vào.

### 9.5. Package `model`: các object dữ liệu chính

Package `model` là phần rất quan trọng vì các object trong đây được truyền qua socket và được DAO map từ database.

Mối liên hệ:

```text
Database table
  -> DAO map ResultSet thành model
  -> Service xử lý model
  -> Controller trả model
  -> AuctionResponse chứa model
  -> Client nhận model và hiển thị UI
```

Hầu hết model kế thừa `Entity` hoặc implement `Serializable`, để có thể serialize qua socket.

| File | Chức năng |
| --- | --- |
| `Entity.java` | Class cha trừu tượng cho các entity có `id`. Implement `Serializable`, nên các class con như `User`, `AuctionItem`, `Wallet`, `AutoBid`, `Notification` có thể truyền qua socket. |
| `User.java` | Class cha cho người dùng. Chứa `username`, `password`, `fullName`, `email`, `role`, `status`, `createdAt`, `updatedAt`. Có `checkPassword()` dùng `PasswordUtil.verifyPassword()`. Field `password` là `transient`, giúp hạn chế serialize password qua socket. |
| `Admin.java` | User role `ADMIN`. Kế thừa `User`, thêm `department`, mặc định `"SYSTEM"`. |
| `Seller.java` | User role `SELLER`. Kế thừa `User`, dùng cho người bán/tạo auction. |
| `Bidder.java` | User role `BIDDER`. Kế thừa `User`, có `bidHistory` dạng danh sách id bid transaction. |
| `Role.java` | Enum role người dùng: `BIDDER`, `SELLER`, `ADMIN`. Server dùng role này để phân quyền request. |
| `Item.java` | Class cha trừu tượng cho item đấu giá. Chứa tên, mô tả, giá khởi điểm, giá hiện tại, thời gian bắt đầu/kết thúc, category, ảnh, thời điểm tạo/cập nhật. |
| `AuctionItem.java` | Model phiên/sản phẩm đấu giá. Kế thừa `Item`, thêm `bidStep`, `sellerId`, `winnerId`, `status`, `antiSnipingExtensionCount`. Được dùng nhiều nhất trong `AuctionService`, `AuctionDAO`, client UI. |
| `AuctionStatus.java` | Enum trạng thái auction: `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`. Scheduler và service dùng để chuyển trạng thái. |
| `BidTransaction.java` | Model một lượt đặt giá. Chứa `auctionId`, `bidderId`, `amount`, `timestamp`, `status`. DAO lưu vào bảng `bids`, client dùng để hiển thị lịch sử bid. |
| `AutoBid.java` | Model rule tự động đặt giá. Chứa `auctionId`, `bidderId`, `maxPrice`, `increment`, `createdAt`, `updatedAt`. `AutobidService` và `AuctionService.applyAutoBids()` dùng model này. |
| `Wallet.java` | Model ví người dùng. Chứa `userId`, `balance`, `reservedBalance`, `createdAt`, `updatedAt`. Có `getAvailableBalance() = balance - reservedBalance`. |
| `TopUpTransaction.java` | Model giao dịch nạp tiền. Chứa user, amount, payment method, status, reference code, transaction time, complete time. |
| `TopUpStatus.java` | Enum trạng thái top-up: `PENDING`, `SUCCESS`, `CANCELLED`. |
| `PaymentMethod.java` | Enum phương thức nạp tiền: `CREDIT_CARD`, `BANK_TRANSFER`, `E_WALLET`, `CASH`. |
| `WalletTransaction.java` | Model log giao dịch ví. Chứa user, type, amount, auctionId, reference, createdAt. |
| `WalletTransactionType.java` | Enum loại giao dịch ví: `TOP_UP`, `RESERVE`, `RELEASE`, `CAPTURE`, `REFUND`. |
| `Notification.java` | Model thông báo. Chứa user nhận, title, content, created_at. |
| `HomepageAnnouncement.java` | Model nội dung trang chủ. Chứa title, summary, details, scheduleText, linkedAuctionId, authorId, createdAt, updatedAt. Có `hasLinkedAuction()` để biết announcement có gắn với auction không. |

Ví dụ liên hệ model với database:

```text
auctions table
  -> AuctionDAOImpl.mapAuction()
  -> AuctionItem
  -> AuctionService xử lý
  -> AuctionResponse trả về client
  -> JavaFX hiển thị AuctionItem
```

Ví dụ liên hệ model với phân quyền:

```text
User.role = Role.ADMIN / SELLER / BIDDER
  -> AuctionSessionManager lưu role trong Session
  -> AuctionRequestHandler.requireRole(...)
  -> cho phép hoặc từ chối action
```

### 9.6. Package `network`: giao thức socket dùng chung

Package `network` là phần cốt lõi của giao tiếp client-server.

Luồng dùng package này:

```text
Client RemoteAuctionClient
  -> tạo AuctionRequest(action, params, sessionToken)
  -> gửi qua ObjectOutputStream

Server AuctionSocketServer
  -> đọc AuctionRequest bằng ObjectInputStream
  -> AuctionRequestHandler đọc action
  -> xử lý và tạo AuctionResponse

Client
  -> đọc AuctionResponse
  -> nếu success thì lấy data
  -> nếu fail thì ném RemoteServerException
```

| File | Chức năng |
| --- | --- |
| `AuctionRequest.java` | Object request client gửi lên server. Chứa `action`, `params`, `sessionToken`. Có `get(key)` để lấy param nhanh từ map. Implement `Serializable` để truyền qua socket. |
| `AuctionResponse.java` | Object response server trả về. Chứa `success`, `data`, `errorType`, `errorMessage`. Có factory `ok(data)` và `fail(Throwable)`. Implement `Serializable`. |
| `AuthenticatedUserResponse.java` | Record response sau đăng nhập thành công. Chứa `User user`, `String sessionToken`, `long expiresAt`. Client nhận object này rồi lưu session token. |
| `NetworkActions.java` | Danh sách action string dùng chung giữa client và server: `AUTH_LOGIN`, `AUCTION_PLACE_BID`, `WALLET_GET`, `HOMEPAGE_SAVE`, `NOTIFICATION_GET`, ... |

Nếu thêm chức năng mới qua socket, thường phải làm các bước:

1. Thêm action mới vào `NetworkActions.java`.
2. Thêm hàm vào API interface nếu đó là chức năng nghiệp vụ mới.
3. Implement ở server service.
4. Implement ở client remote service.
5. Thêm case xử lý trong `AuctionRequestHandler`.

Ví dụ action đặt giá:

```text
NetworkActions.AUCTION_PLACE_BID
  -> client/remote/RemoteAuctionService.placeBid()
  -> server/AuctionRequestHandler case AUCTION_PLACE_BID
  -> AuctionController.placeBid()
  -> AuctionService.placeBid()
```

### 9.7. Package `exception`: lỗi nghiệp vụ dùng chung

Các exception này được dùng để phân biệt từng loại lỗi nghiệp vụ. Server ném exception hoặc controller chuyển exception thành message. Khi server trả `AuctionResponse.fail(ex)`, client có thể đọc `errorType`.

| File | Chức năng |
| --- | --- |
| `AuctionClosedException.java` | Lỗi khi auction không còn mở hoặc không thể đặt giá/đóng theo trạng thái hiện tại. |
| `InvalidBidException.java` | Lỗi khi bid không hợp lệ: giá thấp hơn yêu cầu, bidder đã là winner, ví không đủ tiền, ... |
| `ItemNotFoundException.java` | Lỗi khi không tìm thấy auction, autobid, wallet hoặc entity cần xử lý. |
| `UnauthorizedException.java` | Lỗi không có quyền: chưa đăng nhập, token sai, role không phù hợp, truy cập tài khoản người khác. |
| `ValidationException.java` | Lỗi validate dữ liệu đầu vào: username/email/password sai, giá sai, thời gian sai, bid step sai, top-up amount sai. |

Mối liên hệ:

```text
server/service ném ValidationException
  -> AuctionRequestHandler bắt Throwable
  -> AuctionResponse.fail(ex)
  -> client/remote đọc errorType/errorMessage
  -> UI hiển thị lỗi
```

### 9.8. Package `common`: rule nghiệp vụ chung

| File | Chức năng |
| --- | --- |
| `AuctionRules.java` | Chứa các hằng số rule đấu giá dùng chung: số nhịp admin early-close, giới hạn ảnh 5MB, anti-sniping window 60 giây, tối đa 3 lần kéo dài anti-sniping, bid step tối thiểu 1% và tối đa 10% giá khởi điểm. |

Các service dùng `AuctionRules` để validate và xử lý:

```text
AuctionService.validateImage()
  -> dùng AuctionRules.MAX_IMAGE_BYTES

AuctionService.validateBidStep()
  -> dùng MIN_BID_STEP_PERCENT và MAX_BID_STEP_PERCENT

AuctionService.applyAntiSniping()
  -> dùng ANTI_SNIPING_WINDOW_MS và MAX_ANTI_SNIPING_EXTENSIONS
```

### 9.9. Package `event`: event nội bộ cho auction

Package này tạo cơ chế publish/subscribe đơn giản cho các thay đổi của auction.

| File | Chức năng |
| --- | --- |
| `AuctionEvent.java` | Record mô tả một event auction. Chứa `auctionId`, `type`, `summary`, `occurredAt`, `status`, `currentHighestBid`, `winnerId`, `endTime`. Có factory method cho `bidActivity`, `antiSnipingExtended`, `statusChanged`, `settled`. |
| `AuctionEventBus.java` | Singleton event bus. Cho phép `subscribe(listener)`, `unsubscribe(listener)`, `publish(event)`. Dùng `CopyOnWriteArrayList` để an toàn hơn khi nhiều thread publish/subscribe. |
| `AuctionEventListener.java` | Functional interface có một hàm `onAuctionEvent(AuctionEvent event)`. Class nào muốn nghe event sẽ implement interface này hoặc dùng lambda. |

Mối liên hệ:

```text
AuctionService.placeBid()
  -> eventBus.publish(AuctionEvent.bidActivity(...))

AuctionService.refreshAuctionStatusLocked()
  -> eventBus.publish(AuctionEvent.statusChanged(...))

AuctionService.settleFinishedAuction()
  -> eventBus.publish(AuctionEvent.settled(...))
```

Hiện event bus là cơ chế nội bộ trong JVM. Nó không tự động đẩy realtime qua socket cho client, vì socket hiện tại là request-response một lần.

### 9.10. Package `util`: tiện ích dùng chung

| File | Chức năng |
| --- | --- |
| `PasswordUtil.java` | Hash và verify password. Dùng PBKDF2-HMAC-SHA256 với salt ngẫu nhiên, 120.000 iterations. Có hỗ trợ kiểm tra hash legacy SHA-256 cũ và `needsRehash()` để nâng cấp hash cũ. `User.checkPassword()` và `AuthService` dùng class này. |
| `ImageCompressionUtil.java` | Nén/giải nén byte ảnh bằng DEFLATE và tính compression ratio. Mục đích là giảm kích thước ảnh nếu lưu/truyền ảnh. |

Mối liên hệ của `PasswordUtil`:

```text
AuthService.register()
  -> PasswordUtil.hashPassword(password)
  -> UserDAO.save(user)

AuthService.login()
  -> User.checkPassword(password)
  -> PasswordUtil.verifyPassword(input, storedHash)
```

Điểm bảo mật đáng chú ý: `User.password` là `transient`, nên khi `User` được serialize qua socket, password không được serialize theo cách mặc định.

### 9.11. Package `validation`: validate input dùng chung

| File | Chức năng |
| --- | --- |
| `UserValidator.java` | Validate username, password, email. Username dài 6-20 ký tự, password ít nhất 6 ký tự và có cả chữ lẫn số, email đúng regex cơ bản. |

Mối liên hệ:

```text
AuthService.register()
  -> UserValidator.isValidUsername()
  -> UserValidator.isValidPassword()
  -> UserValidator.isValidEmail()

AuthService.changePassword()
  -> UserValidator.isValidPassword()
```

### 9.12. Mối liên hệ giữa các package trong `core-common`

Có thể hình dung `core-common` theo 4 lớp:

```text
Lớp dữ liệu:
  model

Lớp hợp đồng:
  api
  exception
  common

Lớp giao tiếp:
  network

Lớp hỗ trợ:
  controller
  event
  util
  validation
```

Luồng đặt giá có dùng nhiều package trong `core-common`:

```text
RemoteAuctionService implements AuctionApi
  -> dùng NetworkActions.AUCTION_PLACE_BID
  -> RemoteAuctionClient tạo AuctionRequest
  -> server nhận AuctionRequest
  -> AuctionRequestHandler gọi AuctionController
  -> AuctionController gọi AuctionApi.placeBid()
  -> AuctionService xử lý AuctionItem, BidTransaction, AuctionStatus
  -> nếu lỗi ném InvalidBidException / AuctionClosedException
  -> server trả AuctionResponse
```

Luồng đăng nhập:

```text
RemoteAuthService implements AuthApi
  -> gửi NetworkActions.AUTH_LOGIN
  -> AuctionRequest chứa username/password
  -> AuthController gọi AuthApi.login()
  -> AuthService trả User
  -> AuctionSessionManager tạo AuthenticatedUserResponse
  -> AuctionResponse.ok(AuthenticatedUserResponse)
  -> client lưu sessionToken
```

Luồng ví:

```text
RemoteWalletService implements WalletApi
  -> gửi WALLET_GET / WALLET_TOP_UP / WALLET_TOP_UP_HISTORY
  -> server dùng WalletController
  -> WalletService xử lý Wallet, TopUpTransaction, WalletTransaction
  -> trả model qua AuctionResponse
```

### 9.13. Chức năng từng file trong `core-common` theo danh sách đầy đủ

| File | Chức năng ngắn gọn |
| --- | --- |
| `api/AuctionApi.java` | Interface chức năng đấu giá. |
| `api/AuthApi.java` | Interface chức năng xác thực/tài khoản. |
| `api/AutobidApi.java` | Interface chức năng tự động đặt giá. |
| `api/HomepageContentApi.java` | Interface chức năng nội dung trang chủ. |
| `api/NotificationApi.java` | Interface chức năng thông báo. |
| `api/WalletApi.java` | Interface chức năng ví. |
| `common/AuctionRules.java` | Hằng số rule đấu giá dùng chung. |
| `controller/AuctionController.java` | Gọi `AuctionApi`, chuyển kết quả/lỗi đấu giá. |
| `controller/AuthController.java` | Gọi `AuthApi`, xử lý đăng ký/đăng nhập/user. |
| `controller/AutobidController.java` | Gọi `AutobidApi`, xử lý rule autobid. |
| `controller/HomepageController.java` | Gọi `HomepageContentApi`, xử lý announcement. |
| `controller/NotificationController.java` | Gọi `NotificationApi`, xử lý notification. |
| `controller/WalletController.java` | Gọi `WalletApi`, xử lý wallet/top-up. |
| `event/AuctionEvent.java` | Dữ liệu event khi auction thay đổi. |
| `event/AuctionEventBus.java` | Event bus singleton để publish/subscribe event. |
| `event/AuctionEventListener.java` | Interface listener cho auction event. |
| `exception/AuctionClosedException.java` | Lỗi auction đã đóng/không trong trạng thái phù hợp. |
| `exception/InvalidBidException.java` | Lỗi bid không hợp lệ. |
| `exception/ItemNotFoundException.java` | Lỗi không tìm thấy item/entity. |
| `exception/UnauthorizedException.java` | Lỗi không có quyền/chưa xác thực. |
| `exception/ValidationException.java` | Lỗi dữ liệu đầu vào không hợp lệ. |
| `model/Admin.java` | Model user admin. |
| `model/AuctionItem.java` | Model phiên/sản phẩm đấu giá. |
| `model/AuctionStatus.java` | Enum trạng thái auction. |
| `model/AutoBid.java` | Model rule tự động đặt giá. |
| `model/Bidder.java` | Model user bidder. |
| `model/BidTransaction.java` | Model một lượt đặt giá. |
| `model/Entity.java` | Class cha có `id`, implement `Serializable`. |
| `model/HomepageAnnouncement.java` | Model announcement trang chủ. |
| `model/Item.java` | Class cha cho item đấu giá. |
| `model/Notification.java` | Model thông báo. |
| `model/PaymentMethod.java` | Enum phương thức thanh toán/nạp tiền. |
| `model/Role.java` | Enum role user. |
| `model/Seller.java` | Model user seller. |
| `model/TopUpStatus.java` | Enum trạng thái nạp tiền. |
| `model/TopUpTransaction.java` | Model giao dịch nạp tiền. |
| `model/User.java` | Class cha cho user, chứa thông tin tài khoản và check password. |
| `model/Wallet.java` | Model ví, số dư và số tiền đang giữ. |
| `model/WalletTransaction.java` | Model log giao dịch ví. |
| `model/WalletTransactionType.java` | Enum loại giao dịch ví. |
| `network/AuctionRequest.java` | Request socket client gửi server. |
| `network/AuctionResponse.java` | Response socket server trả client. |
| `network/AuthenticatedUserResponse.java` | Response đăng nhập thành công gồm user, token, hạn token. |
| `network/NetworkActions.java` | Danh sách action socket dùng chung. |
| `util/ImageCompressionUtil.java` | Nén/giải nén ảnh dạng byte. |
| `util/PasswordUtil.java` | Hash, verify và kiểm tra rehash password. |
| `validation/UserValidator.java` | Validate username, password, email. |

### 9.14. Folder test của `core-common`

`core-common/src/test` chứa unit test cho các phần dùng chung. Các test này quan trọng vì nếu `core-common` sai thì cả client và server đều có thể lỗi.

| File test | Chức năng |
| --- | --- |
| `event/AuctionEventBusTest.java` | Test cơ chế subscribe/publish/unsubscribe của `AuctionEventBus`. Đảm bảo listener nhận đúng event và không nhận sau khi unsubscribe. |
| `network/AuctionRequestResponseTest.java` | Test `AuctionRequest` và `AuctionResponse`: action, params, session token, success response, fail response. |
| `network/UserSerializationTest.java` | Test serialize user qua network. Điểm quan trọng là password trong `User` là `transient`, tránh gửi password qua socket. |
| `util/PasswordUtilTest.java` | Test hash password, verify password, sai password, hash cũ và logic `needsRehash()`. |
| `validation/UserValidatorTest.java` | Test validate username, password, email. |

Mối liên hệ test:

```text
core-common test pass
  -> model/network/util/validation ổn định
  -> client và server có nền tảng chung ổn định hơn
```

### 9.15. Cách đọc `core-common` nếu mới bắt đầu

Thứ tự đọc hợp lý:

1. `model/Role.java`, `model/User.java`, `model/Admin.java`, `model/Seller.java`, `model/Bidder.java`
2. `model/AuctionItem.java`, `model/AuctionStatus.java`, `model/BidTransaction.java`
3. `model/Wallet.java`, `model/TopUpTransaction.java`, `model/WalletTransaction.java`
4. `network/NetworkActions.java`
5. `network/AuctionRequest.java`, `network/AuctionResponse.java`
6. `api/AuthApi.java`, `api/AuctionApi.java`, `api/WalletApi.java`
7. `controller/AuthController.java`, `controller/AuctionController.java`, `controller/WalletController.java`
8. `common/AuctionRules.java`
9. `exception/*.java`
10. `util/PasswordUtil.java`, `validation/UserValidator.java`
11. `event/AuctionEvent.java`, `event/AuctionEventBus.java`

Lý do đọc theo thứ tự này: hiểu dữ liệu trước, hiểu giao thức socket sau, rồi mới hiểu hợp đồng API và controller.

## 10. Ví Dụ Đầy Đủ: Bidder Đặt Giá

Đây là luồng đầy đủ để dễ hình dung:

```text
1. Bidder bấm nút Đặt Giá trên JavaFX.
2. UI controller gọi RemoteAuctionService.placeBid(auctionId, bidderId, amount).
3. RemoteAuctionService gọi:
   client.call(AUCTION_PLACE_BID, "auctionId", auctionId, "bidderId", bidderId, "amount", amount)
4. RemoteAuctionClient tạo AuctionRequest với sessionToken hiện tại.
5. Client mở socket đến server host:port.
6. Client ghi object AuctionRequest bằng ObjectOutputStream.
7. AuctionSocketServer accept connection.
8. Server đọc object bằng ObjectInputStream và filter class.
9. AuctionRequestHandler thấy action = AUCTION_PLACE_BID.
10. Handler gọi requireRole(BIDDER).
11. AuctionSessionManager check token và lấy userId thật từ session.
12. Handler gọi AuctionController.placeBid(auctionId, principal.userId(), amount).
13. AuctionService lock auctionId.
14. AuctionService load AuctionItem từ AuctionDAO.
15. Service check auction RUNNING, amount hợp lệ, bidder có đủ tiền.
16. WalletService check available balance.
17. AuctionService thêm bid mới, chạy autobid nếu có.
18. WalletService release tiền winner cũ và reserve tiền winner mới.
19. AuctionDAO.saveBid() lưu lịch sử bid.
20. AuctionDAO.updateAuctionState() cập nhật winner/currentHighestBid/endTime/status.
21. Server trả AuctionResponse.ok("SUCCESS").
22. Client đọc response.
23. UI cập nhật danh sách auction/bid.
```

## 11. Nên Đọc File Theo Thứ Tự Nào?

Nếu mới bắt đầu tìm hiểu, nên đọc theo thứ tự sau:

1. `core-common/src/main/java/userauth/network/NetworkActions.java`
2. `core-common/src/main/java/userauth/network/AuctionRequest.java`
3. `core-common/src/main/java/userauth/network/AuctionResponse.java`
4. `client/src/main/java/userauth/remote/RemoteAuctionClient.java`
5. `client/src/main/java/userauth/remote/RemoteAuthService.java`
6. `client/src/main/java/userauth/remote/RemoteAuctionService.java`
7. `server/src/main/java/userauth/server/AuctionServerMain.java`
8. `server/src/main/java/userauth/server/AuctionSocketServer.java`
9. `server/src/main/java/userauth/server/AuctionRequestHandler.java`
10. `server/src/main/java/userauth/server/ServerContext.java`
11. `server/src/main/java/userauth/service/AuthService.java`
12. `server/src/main/java/userauth/service/AuctionService.java`
13. `server/src/main/java/userauth/service/WalletService.java`
14. `server/src/main/java/userauth/dao/*DAO.java`
15. `server/src/main/java/userauth/dao/*DAOImpl.java`
16. `server/src/main/java/userauth/database/DatabaseInitializer.java`

Thứ tự này giúp bạn đi từ giao thức socket đến luồng xử lý backend, rồi mới xuống database.

## 12. Tóm Tắt Để Nhớ Nhanh

```text
client/remote
  -> đóng gói request và gửi socket

core-common/network
  -> định nghĩa AuctionRequest, AuctionResponse, NetworkActions

server/server
  -> lắng nghe socket, check session/role, điều phối request

server/service
  -> xử lý nghiệp vụ thật

server/dao
  -> SQL đọc/ghi database

server/database
  -> cấu hình, connection pool, tạo/đồng bộ bảng
```

Kết luận: project này đã tách đúng hướng client-server. Client chỉ là UI và socket caller. Server là nơi duy nhất nắm giữ database, session, permission và business logic.
