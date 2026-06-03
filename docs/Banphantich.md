# Bản phân tích project

## 1. Tổng quan hệ thống

Project là hệ thống đấu giá trực tuyến được xây dựng bằng Java 21 theo kiến trúc client-server. Client là ứng dụng JavaFX, server là Java Socket Server, database là MySQL/Akamai DB.

Mục tiêu chính của hệ thống:

- Cho phép người dùng đăng ký, đăng nhập và sử dụng tài khoản theo vai trò.
- Cho seller tạo, sửa, xóa, đóng và quyết toán phiên đấu giá.
- Cho bidder xem phiên đấu giá, đặt giá, cấu hình auto-bid và quản lý ví.
- Cho admin quản lý user, quản lý auction, đóng sớm auction và quản lý nội dung trang chủ.
- Đảm bảo client không truy cập database trực tiếp. Mọi thao tác dữ liệu đi qua server.

Kiến trúc tổng quát:

```text
JavaFX UI
-> Controller UI
-> Remote service
-> RemoteAuctionClient
-> Socket request/response
-> AuctionSocketServer
-> AuctionRequestHandler
-> Controller chung
-> Service
-> DAO
-> Database
```

## 2. Cấu trúc module Maven

Project gồm 3 module chính:

```text
auction-house
|-- core-common
|-- client
`-- server
```

### 2.1. Module `core-common`

Vai trò:

- Chứa code dùng chung giữa client và server.
- Định nghĩa hợp đồng API, model, request/response socket, validation và util.
- Là module bắt buộc phải đồng bộ version giữa client và server.

Nếu client và server dùng lệch version `core-common`, các object serialize qua socket có thể không tương thích.

### 2.2. Module `client`

Vai trò:

- Hiển thị giao diện JavaFX.
- Nhận thao tác người dùng.
- Gọi server qua socket bằng các class `Remote*Service`.
- Không chứa DAO và không truy cập database.

Entry point:

```text
userauth.ClientLauncher
```

### 2.3. Module `server`

Vai trò:

- Lắng nghe socket từ client.
- Kiểm tra session, role và quyền thao tác.
- Xử lý nghiệp vụ đấu giá, ví, tài khoản, notification.
- Truy cập database qua DAO.

Entry point:

```text
userauth.server.AuctionServerMain
```

## 3. Phân tích thư mục gốc

### 3.1. `pom.xml`

File Maven cha khai báo:

- Group/artifact/version của project.
- 3 module con: `core-common`, `client`, `server`.
- Java version: 21.
- JavaFX version: 21.
- JUnit version.
- MySQL connector version.
- Maven compiler, surefire và enforcer plugin.

Đây là điểm bắt đầu khi build/test toàn bộ project.

### 3.2. `README.md`

Chứa hướng dẫn tổng quan:

- Công nghệ sử dụng.
- Yêu cầu môi trường.
- Cách build/test.
- Cách chạy server/client.
- Cách triển khai VPS.
- Các lỗi thường gặp.

### 3.3. `docs/`

Chứa tài liệu phân tích, thiết kế, triển khai và bảo mật:

- Kiến trúc client-server.
- Hướng dẫn deploy.
- Bảo mật socket.
- Tối ưu database/socket/cache.
- Giải thích tách client-server.

File phân tích hiện tại được tạo tại `docs/Banphantich.md` để làm tài liệu đọc code tổng thể.

### 3.4. `scripts/`

Chứa các script hỗ trợ:

- Deploy server lên VPS.
- Restart server.
- Tách client/server thành gói riêng.
- Chọn JDK 21.

### 3.5. `database_indexes.sql`

Chứa các index tối ưu truy vấn:

- Auction theo status/time/seller.
- Bid theo auction/time.
- Auto-bid theo auction/bidder.
- Top-up và wallet transaction theo user/time.

## 4. Phân tích `core-common`

## 4.1. Package `userauth.api`

Package này định nghĩa các interface chức năng chính của hệ thống. Đây là lớp hợp đồng chung giữa client và server. Client gọi API thông qua remote service, còn server hiện thực API bằng service thật.

### `AuthApi`

Quản lý tài khoản:

- `register`
- `login`
- `logout`
- `getUserById`
- `getAllUsers`
- `changePassword`
- `updateProfile`
- `toggleUserStatus`
- `deleteUserAccount`

Luồng chính của `AuthApi` liên quan đến đăng ký, đăng nhập, đổi mật khẩu, cập nhật hồ sơ và quản trị user.

### `AuctionApi`

Quản lý đấu giá:

- Tạo/sửa/xóa auction.
- Lấy danh sách auction.
- Lấy bid history.
- Đặt giá.
- Đóng auction.
- Admin early-close.
- Mark paid/cancel finished auction.
- Refresh status auction.

Đây là API trung tâm của nghiệp vụ đấu giá.

### `WalletApi`

Quản lý ví:

- Lấy ví người dùng.
- Tạo yêu cầu nạp tiền.
- Lấy lịch sử nạp tiền.

Ví được dùng để kiểm soát số dư, số tiền bị giữ và lịch sử giao dịch.

### `AutobidApi`

Quản lý auto-bid:

- Tạo auto-bid.
- Sửa auto-bid.
- Xóa auto-bid.
- Lấy auto-bid theo bidder.
- Lấy auto-bid theo id.

Auto-bid giúp bidder tự động đặt giá theo mức tối đa và bước nhảy đã cấu hình.

### `NotificationApi`

Quản lý thông báo:

- Tạo notification.
- Lấy notification của user.
- Xóa một notification.
- Xóa tất cả notification của user.

Thông báo được dùng cho các sự kiện như thắng đấu giá, bị vượt giá, đấu giá kết thúc hoặc thông báo chung.

### `HomepageContentApi`

Quản lý nội dung trang chủ:

- Lấy tất cả announcement.
- Lưu announcement.
- Xóa announcement.

API này chủ yếu phục vụ màn hình admin quản lý nội dung hiển thị ở trang chủ.

## 4.2. Package `userauth.model`

Package này chứa các đối tượng dữ liệu chính. Hầu hết model implement `Serializable` thông qua `Entity` hoặc trực tiếp để có thể truyền qua socket.

### User model

Các class liên quan:

- `User`
- `Admin`
- `Seller`
- `Bidder`
- `Role`

`User` gồm:

- `id`
- `username`
- `password`
- `fullName`
- `email`
- `role`
- `status`
- `createdAt`
- `updatedAt`

Điểm quan trọng:

- Trường `password` là `transient`, nên không bị serialize về client.
- `checkPassword` dùng `PasswordUtil.verifyPassword`.
- Role gồm `BIDDER`, `SELLER`, `ADMIN`.
- Status thường dùng `ACTIVE` và `BLOCKED`.

### Auction model

Các class liên quan:

- `Item`
- `AuctionItem`
- `AuctionStatus`
- `BidTransaction`

`AuctionItem` gồm:

- Thông tin sản phẩm: name, description, category, image.
- Giá: `startPrice`, `currentHighestBid`, `bidStep`.
- Thời gian: `startTime`, `endTime`.
- Chủ sở hữu: `sellerId`.
- Người thắng: `winnerId`.
- Trạng thái: `OPEN`, `RUNNING`, `FINISHED`, `PAID`, `CANCELED`.
- Số lần gia hạn anti-sniping.

`BidTransaction` lưu:

- Auction id.
- Bidder id.
- Số tiền bid.
- Thời điểm bid.
- Status bid.

### Auto-bid model

`AutoBid` gồm:

- Auction id.
- Bidder id.
- Max price.
- Increment.
- Created/updated time.

Auto-bid được server xử lý khi auction đang chạy hoặc khi có bid mới.

### Wallet model

Các class liên quan:

- `Wallet`
- `WalletTransaction`
- `WalletTransactionType`
- `TopUpTransaction`
- `TopUpStatus`
- `PaymentMethod`

`Wallet` có:

- `balance`: tổng tiền.
- `reservedBalance`: tiền đang bị giữ.
- `availableBalance = balance - reservedBalance`.

`WalletTransactionType` gồm:

- `TOP_UP`
- `RESERVE`
- `RELEASE`
- `CAPTURE`
- `REFUND`

### Notification và homepage model

- `Notification`: thông báo cho user hoặc thông báo chung với `user_id = 0`.
- `HomepageAnnouncement`: nội dung admin đăng lên trang chủ, có thể liên kết với một auction.

## 4.3. Package `userauth.network`

Đây là phần giao tiếp socket.

### `NetworkActions`

Chứa hằng số action cho toàn bộ request socket.

Nhóm action chính:

- `AUTH_*`
- `AUCTION_*`
- `AUTOBID_*`
- `WALLET_*`
- `HOMEPAGE_*`
- `NOTIFICATION_*`

Khi thêm chức năng mới, thường phải thêm action ở đây.

### `AuctionRequest`

Object client gửi lên server:

```text
action
params
sessionToken
```

`params` là `Map<String, Object>`.

### `AuctionResponse`

Object server trả về client:

```text
success
data
errorType
errorMessage
```

Nếu có lỗi, server gọi `AuctionResponse.fail(Throwable)`.

### `AuthenticatedUserResponse`

Trả về sau login thành công:

```text
User user
String sessionToken
long expiresAt
```

Client lưu `sessionToken` trong `RemoteAuctionClient`.

## 4.4. Package `userauth.controller`

Controller chung dùng cho cả client và server.

Vai trò:

- Bọc quanh API service.
- Chuyển exception nghiệp vụ thành chuỗi `"SUCCESS"` hoặc error message.
- Tạo lớp trung gian giữa UI/request handler và service.

Điểm cần chú ý:

- Một số method trả về `String`.
- Một số method trả về object/list.
- Khi viết thêm chức năng, cần giữ consistency kiểu trả về để remote service xử lý đúng.

## 4.5. Package `userauth.common`

`AuctionRules` chứa các rule quan trọng:

- `ADMIN_EARLY_CLOSE_COUNTS = 3`
- `MAX_IMAGE_BYTES = 5 MB`
- `ANTI_SNIPING_WINDOW_MS = 60_000`
- `MAX_ANTI_SNIPING_EXTENSIONS = 3`
- Bid step tối thiểu 1% start price.
- Bid step tối đa 10% start price.

Đây là nơi nên xem trước khi chỉnh rule nghiệp vụ liên quan đến đấu giá.

## 4.6. Package `userauth.util`

### `PasswordUtil`

- Hash password bằng PBKDF2-SHA256.
- Mỗi hash có salt riêng.
- Có check `needsRehash`.
- Có verify legacy SHA-256 hash.

### `ImageCompressionUtil`

- Nén/giải nén byte array ảnh.
- Hữu ích nếu cần giảm payload ảnh qua socket.

## 4.7. Package `userauth.validation`

`UserValidator` kiểm tra:

- Username.
- Password.
- Email.

Điểm cần chú ý:

- Code yêu cầu username từ 6 đến 20 ký tự.
- Một số message trong service nói 3 đến 20 ký tự, cần đồng bộ lại nếu muốn chuẩn hóa UX.

## 4.8. Package `userauth.event`

Gồm:

- `AuctionEvent`
- `AuctionEventBus`
- `AuctionEventListener`

Dùng để phát event nội bộ khi:

- Có bid mới.
- Anti-sniping gia hạn.
- Status auction thay đổi.
- Auction được settle.

Client dashboard có đăng ký observer để cập nhật UI nhanh hơn ngoài polling.

## 5. Phân tích server

## 5.1. Package `userauth.server`

### `AuctionServerMain`

Entry point server.

Đọc cấu hình:

- `app.server.port` hoặc `APP_SERVER_PORT`.
- `app.server.bind.host` hoặc `APP_SERVER_BIND_HOST`.
- `app.server.tls.enabled` hoặc `APP_SERVER_TLS_ENABLED`.

Sau đó:

- Tạo `ServerContext`.
- Tạo `AuctionSocketServer`.
- Đăng ký shutdown hook.
- Start server.

### `ServerContext`

Khởi tạo dependency phía server:

- `DatabaseInitializer.initialize()`.
- DAO: `UserDAOImpl`, `AuctionDAOImpl`, `AutoBidDAOImpl`, `WalletDAOImpl`, `NotificationDAOImpl`.
- Service: `AuthService`, `AuctionService`, `WalletService`, `AutobidService`, `HomepageContentService`, `NotificationService`.
- Controller: `AuthController`, `AuctionController`, `WalletController`, `AutobidController`, `HomepageController`, `NotificationController`.
- Scheduler: `AuctionScheduler`.

Điểm quan trọng:

- Chỉ server mới tạo DAO.
- Khi start, service gọi `reconcileReservedBalances`.

### `AuctionSocketServer`

Mở port socket và xử lý client.

Có các cơ chế bảo vệ:

- Thread pool giới hạn.
- Queue giới hạn.
- Read timeout 10 giây.
- `ObjectInputFilter` giới hạn class được deserialize.
- Giới hạn request bytes và array length.
- Hỗ trợ TLS nếu bật cấu hình.

Mỗi connection xử lý một `AuctionRequest` và trả một `AuctionResponse`.

### `AuctionRequestHandler`

Router của server.

Nhiệm vụ:

- Kiểm tra action.
- Kiểm tra session token.
- Kiểm tra role.
- Kiểm tra self-access.
- Gọi controller/service tương ứng.

Đây là lớp bảo mật quan trọng nhất của server.

Ví dụ:

- `AUCTION_CREATE` yêu cầu `SELLER`.
- `AUCTION_PLACE_BID` yêu cầu `BIDDER`.
- `AUTH_ALL_USERS` yêu cầu `ADMIN`.
- `WALLET_GET` yêu cầu user chỉ được xem ví của chính mình.

Điểm tốt:

- Nhiều action không tin `userId` từ client mà lấy từ principal trong session.
- Khi admin khóa/xóa user, session của target user bị invalidate.

### `AuctionSessionManager`

Quản lý session:

- Tạo token ngẫu nhiên 32 byte.
- Timeout mặc định 8 giờ.
- Tối đa 5 session mỗi user.
- Cho phép invalidate token hoặc invalidate tất cả session của user.

## 5.2. Package `userauth.service`

### `AuthService`

Chức năng:

- Đăng ký user.
- Login.
- Đổi mật khẩu.
- Cập nhật profile.
- Khóa/mở khóa tài khoản.
- Xóa tài khoản.

Điểm tốt:

- Password được hash bằng PBKDF2.
- Login reject user `BLOCKED`.
- Khi login bằng hash cũ, service có thể upgrade hash.
- User mới có role khác admin sẽ được khởi tạo wallet.

Điểm cần chú ý:

- Admin không được tạo từ màn hình register.
- Admin không được lock admin khác từ UI/service.

### `AuctionService`

Đây là service phức tạp nhất.

Chức năng:

- Tạo auction.
- Sửa auction khi chưa có bid.
- Xóa auction chưa có bid.
- Cancel auction đã có bid.
- Đặt giá.
- Xử lý auto-bid.
- Xử lý anti-sniping.
- Đóng auction thủ công.
- Đóng auction theo thời gian.
- Admin early-close.
- Mark paid.
- Cancel finished auction.
- Reconcile reserved balance.

Luồng đặt giá:

```text
Lấy lock theo auction
-> Load auction
-> Refresh status nếu cần
-> Kiểm tra RUNNING
-> Kiểm tra bidder không phải winner hiện tại
-> Kiểm tra minimum bid
-> Kiểm tra ví đủ tiền
-> Tạo bid pending
-> Chạy auto-bid
-> Apply anti-sniping
-> Apply reservation transition
-> Lưu bid sequence
-> Update auction state
-> Publish event
```

Điểm tốt:

- Có lock theo auction để tránh race khi nhiều bid vào cùng lúc.
- Có lock refresh status riêng.
- Có test concurrent bidding.
- Có reconcile reserved balance để tự sửa một số lệch dữ liệu.

Rủi ro:

- Các thao tác wallet, save bid và update auction không nằm trong một database transaction duy nhất.
- Nếu server crash giữa các bước, có thể lệch `wallet.reservedBalance`, `bids`, `auction.winnerId`.
- Giá trị tiền trong auction/bid dùng `double`, wallet dùng `long`, DB dùng `DECIMAL`.

### `WalletService`

Chức năng:

- Khởi tạo wallet.
- Lấy wallet.
- Tạo top-up request.
- Kiểm tra tiền khả dụng cho bid.
- Reserve/release/capture/refund tiền.
- Reconcile reserved balance.
- Ghi audit wallet transaction.

Luồng reserve:

```text
Bidder mới đặt giá
-> Giữ tiền bidder mới
-> Nếu có winner cũ thì nhả tiền winner cũ
```

Luồng capture:

```text
Auction kết thúc có winner
-> Trừ reservedBalance
-> Trừ balance
-> Ghi transaction CAPTURE
```

Điểm tốt:

- Có lock theo user.
- Khi thao tác 2 user, lock theo thứ tự id tăng dần để giảm deadlock.
- Có validate `reservedBalance <= balance`.

Rủi ro:

- Transaction wallet và transaction auction tách nhau.
- `createTopUpRequest` hiện đang auto-confirm thành công, chưa có payment gateway thật.

### `AutobidService`

Chức năng:

- Tạo hoặc update auto-bid theo auction/bidder.
- Validate max price và increment.
- Increment phải >= bid step của auction.
- Sau khi tạo/sửa/xóa auto-bid, gọi `auctionService.triggerAutoBids`.

Điểm cần chú ý:

- Auto-bid có unique index theo `(auction_id, bidder_id)`.
- Xóa auto-bid có fallback xóa theo auction/bidder nếu id sai.

### `NotificationService`

Chức năng:

- Tạo notification.
- Lấy notification của user.
- Xóa notification.
- Xóa tất cả notification của user.

Điểm cần chú ý:

- `user_id = 0` được xem như notification chung.
- `findNotificationToUser` trả về notification của user và notification chung.

### `HomepageContentService`

Chức năng:

- CRUD homepage announcement.
- Sắp xếp theo `updatedAt` mới nhất.
- Có thể link announcement với auction.

### `AuctionScheduler`

Thread nền chạy mỗi 1 giây:

- Gọi `auctionService.refreshAuctionStatuses()`.
- Giúp auction tự động chuyển trạng thái theo thời gian.

### `AuctionCache`

Là cache TTL cho auction data.

Điểm cần chú ý:

- Có test riêng.
- Hiện chưa thấy được wire rõ ràng vào `AuctionService`/`ServerContext`.
- Nếu không dùng, đây là code tiềm năng bị bỏ quên.

### `AuctionSettlementHandlerFactory`

Áp dụng pattern factory/strategy cho settlement:

- `PAID`: yêu cầu winner hợp lệ, set status `PAID`.
- `CANCELED`: clear winner, set status `CANCELED`.

## 5.3. Package `userauth.dao`

DAO dùng JDBC trực tiếp và `PreparedStatement`.

### `UserDAOImpl`

Quản lý bảng `users`.

Chức năng đặc biệt:

- `deleteById` tự xóa dữ liệu liên quan trong nhiều bảng.
- Xóa auction của seller.
- Xóa auto-bid/bid của bidder.
- Recalculate auction sau khi xóa bid của user.
- Xóa wallet, top-up, wallet transaction.

Rủi ro:

- Logic cascade thủ công phức tạp.
- Nếu schema thêm bảng mới liên quan user, cần cập nhật method này.

### `AuctionDAOImpl`

Quản lý bảng `auctions` và `bids`.

Điểm đáng chú ý:

- `findAllAuctionSummaries` trả `NULL AS image_data` để giảm payload.
- `findStatusRefreshCandidates` chỉ lấy auction cần refresh theo thời gian.
- `updateAuctionState` chỉ update state fields, không update toàn bộ auction.

### `WalletDAOImpl`

Quản lý:

- `wallets`
- `topup_transactions`
- `wallet_transactions`

Có validate wallet trước khi save/update.

### `AutoBidDAOImpl`

Quản lý bảng `auto_bids`.

Có các query:

- Theo id.
- Theo auction/bidder.
- Theo auction.
- Theo bidder.

### `NotificationDAOImpl`

Quản lý bảng `notifications`.

Điểm đáng chú ý:

- Query notification theo user lấy cả `user_id = 0`.
- Xóa notification chỉ xóa được notification có `user_id` đúng bằng user. Notification chung `user_id = 0` không bị user thường xóa riêng.

### `HomepageAnnouncementDAOImpl`

Quản lý bảng `homepage_announcements`.

Nếu `linkedAuctionId <= 0`, DAO set null vào database.

## 5.4. Package `userauth.database`

### `DatabaseConfig`

Đọc config từ:

1. JVM system property.
2. Environment variable.
3. File `database.properties`.

Các key quan trọng:

- `db.type`
- `db.host`
- `db.port`
- `db.name`
- `db.username`
- `db.password`
- `db.sslMode`
- `db.createDatabaseIfMissing`

### `DatabaseConnection`

Quản lý connection pool thủ công:

- Max connection từ 2 đến 6.
- Blocking queue cho idle connections.
- Validate connection idle.
- Wrap connection bằng proxy để recycle khi close.

Rủi ro:

- Pool tự viết cần test kỹ với lỗi network DB.
- Nếu workload tăng cao, max 6 connection có thể là nút thắt.

### `DatabaseInitializer`

Tạo/sync schema khi server start.

Bảng chính:

- `users`
- `auctions`
- `bids`
- `homepage_announcements`
- `auto_bids`
- `wallets`
- `topup_transactions`
- `wallet_transactions`
- `notifications`

Điểm tốt:

- Có support MySQL và PostgreSQL.
- Có tạo index.
- Có ignore một số lỗi schema đã tồn tại.

Rủi ro:

- DDL migration nằm trong code, khó kiểm soát version schema dài hạn.
- Một số câu SQL MySQL như `CREATE INDEX IF NOT EXISTS` có thể phụ thuộc version DB.

## 6. Phân tích client

## 6.1. Entry point

### `ClientLauncher`

Class main nhỏ, gọi `ClientMain.main(args)`.

### `ClientMain`

Khởi tạo:

- `RemoteAuctionClient`.
- Các `Remote*Service`.
- Các controller chung.
- `AuthFrame`.

Tất cả remote service share cùng một `RemoteAuctionClient`, nên share cùng session token.

## 6.2. Package `userauth.remote`

### `RemoteAuctionClient`

Nhiệm vụ:

- Tạo `AuctionRequest`.
- Gắn session token vào request.
- Mở socket.
- Gửi object request.
- Đọc object response.
- Nếu response fail, throw `RemoteServerException`.

Bảo vệ:

- Có response `ObjectInputFilter`.
- Có timeout.
- Hỗ trợ TLS nếu config bật.

### `RemoteClientConfig`

Đọc config server:

- `app.server.host` hoặc `APP_SERVER_HOST`.
- `app.server.port` hoặc `APP_SERVER_PORT`.
- `app.server.tls.enabled` hoặc `APP_SERVER_TLS_ENABLED`.

Default host hiện là VPS IP.

### `RemoteAuthService`

Map `AuthApi` sang socket action.

Điểm quan trọng:

- Sau login thành công, set session token vào `RemoteAuctionClient`.
- Logout sẽ gọi server nếu có session, sau đó clear local token.

### `RemoteAuctionService`

Map `AuctionApi` sang socket action.

Điểm cần chú ý:

- Một số error được map về exception cụ thể.
- Một số method trả về string `"SUCCESS"` rồi remote service chuyển thành exception nếu khác success.

### `RemoteWalletService`

Map wallet action.

`createTopUpRequest` parse chuỗi:

```text
SUCCESS: Transaction ID <id>
```

Nếu sau này đổi protocol, cần sửa cả server/controller và remote service.

### `RemoteAutobidService`

Map auto-bid action.

Điểm cần chú ý:

- `createAutobid` map nhiều lỗi server về `ValidationException`.

### `RemoteHomepageContentService`

Map homepage action.

Có `synchronized`, nhưng do request socket mỗi lần riêng nên ý nghĩa chính là tránh concurrent call trong object service.

### `RemoteNotificationService`

Map notification action.

Một số lỗi server được wrap thành `RuntimeException`, có thể cần cải thiện nếu muốn UI hiển thị lỗi đẹp hơn.

## 6.3. Package `userauth.gui.fxml.shell`

### `AuthFrame`

Là lớp điều hướng màn hình chính.

Quản lý:

- Stage/Scene.
- Home view.
- Login/register view.
- Admin dashboard.
- Admin homepage manager.
- Seller dashboard.
- Bidder dashboard.
- Dialog đổi mật khẩu, profile, top-up, inbox, bid history.
- Session monitor mỗi 10 giây.

Điểm tốt:

- View được load một lần và reuse.
- Khi chuyển view có deactivate live views.
- Có check user bị lock/xóa để force logout.

### `AppShellController`

Quản lý vùng content chính bằng `StackPane`.

Có animation khi switch view.

## 6.4. Package `userauth.gui.fxml.auth`

### `LoginViewController`

Chức năng:

- Nhập username/password.
- Show/hide password.
- Gọi `authController.login` bằng `UiAsync`.
- Khi thành công, gọi handler chuyển sang dashboard theo role.

### `RegisterViewController`

Chức năng:

- Đăng ký bidder/seller.
- Không expose role admin.
- Validate live username/fullname/email/password/confirm password.
- Gọi `authController.registerGUI`.

## 6.5. Package `userauth.gui.fxml.home`

### `HomeViewController`

Chức năng:

- Hiển thị landing page.
- Hiển thị auction cards.
- Hiển thị admin announcement.
- Load summary trước, load chi tiết ảnh khi cần.
- Refresh theo timeline.

### Card controllers

- `HomeAuctionCardController`: hiển thị auction card.
- `HomeAnnouncementCardController`: hiển thị announcement card.
- `HomeEmptyCardController`: empty state.

## 6.6. Package `userauth.gui.fxml.bidder`

### `BidderDashboardViewController`

Đây là controller lớn nhất project.

Chức năng:

- Hiển thị danh sách auction.
- Lọc/search theo keyword/status.
- Hiển thị chi tiết auction.
- Đặt bid.
- Tạo/sửa/xóa auto-bid.
- Hiển thị ví.
- Top-up wallet.
- Hiển thị bid history.
- Hiển thị inbox.
- Đổi mật khẩu/profile.
- Refresh realtime bằng timeline.
- Xử lý auction event.
- Báo winner/bị vượt giá.

Điểm tốt:

- Có ticket refresh để tránh response cũ ghi đè response mới.
- Có logic tránh refresh phá khi user đang scroll/typing.
- Có lazy load image detail.

Rủi ro:

- 1924 dòng, quá nhiều trách nhiệm trong một class.
- Nên tách thành các phần: auction list presenter, bid form controller, autobid form controller, wallet panel controller, notification/winner overlay helper.

## 6.7. Package `userauth.gui.fxml.seller`

### `SellerDashboardViewController`

Chức năng:

- Tạo auction.
- Sửa auction.
- Xóa/cancel auction.
- Đóng auction.
- Mark paid.
- Cancel finished auction.
- Chọn ảnh local.
- Preview bid step/schedule/image.
- Refresh danh sách auction của seller.
- Inbox, profile, change password.

Điểm cần chú ý:

- Khi chọn ảnh local, controller đọc byte image để gửi lên server.
- Form schedule dùng `DatePicker` và text time.
- Bid step validate theo rule server.

Rủi ro:

- 1072 dòng, nên tách form handling/image handling/schedule handling/table handling.

## 6.8. Package `userauth.gui.fxml.admin`

### `AdminDashboardViewController`

Chức năng:

- Xem danh sách user.
- Lock/unlock user.
- Delete account.
- Xem danh sách auction.
- Delete auction.
- Start/cancel early-close countdown.
- Xem metrics và chart.
- Chuyển sang homepage manager.

Điểm quan trọng:

- UI chặn lock admin account.
- Server cũng có check quyền, UI chỉ là lớp bảo vệ thêm.

### `AdminHomepageViewController`

Chức năng:

- Tạo/sửa/xóa homepage announcement.
- Link announcement với auction.
- Preview announcement.
- Quản lý upcoming auction hiện trên homepage.

## 6.9. Package `userauth.gui.fxml.dialog`

Dialog gồm:

- `ChangePasswordDialogController`
- `ProfileDialogController`
- `TopUpDialogController`
- `InboxDialogController`
- `BidHistoryDialogController`
- `ModalMessageController`
- `TextInputDialogController`
- `ToastNotificationController`

Vai trò:

- Tách các popup/modal khỏi dashboard.
- Gọi controller/service bằng `UiAsync` khi cần.
- Dùng `NotificationUtil` để hiện success/error/confirm.

## 6.10. Package `userauth.gui.fxml.shared`

Tiện ích UI:

- `UiAsync`: chạy task nền và callback về JavaFX thread.
- `FxmlRuntime`: load FXML, tạo modal dialog.
- `UiText`: dịch text Anh/Việt.
- `UiInput`: parse/format input tiền, decimal, integer.
- `UiEffects`: animation.
- `NotificationUtil`: toast/modal notification.
- `AuctionImageUtil`: load/cache ảnh auction.
- `AuctionViewFormatter`: format tiền, thời gian, status, bid step.

## 7. Phân tích database

## 7.1. Bảng `users`

Lưu tài khoản:

- Username unique.
- Email unique.
- Password hash.
- Role.
- Status.

## 7.2. Bảng `auctions`

Lưu auction:

- Thông tin sản phẩm.
- Giá khởi điểm/current.
- Thời gian start/end.
- Ảnh source/data.
- Bid step.
- Seller/winner.
- Status.
- Anti-sniping extension count.

## 7.3. Bảng `bids`

Lưu lịch sử bid:

- Auction.
- Bidder.
- Amount.
- Bid time.
- Status.

## 7.4. Bảng `auto_bids`

Lưu cấu hình auto-bid:

- Auction.
- Bidder.
- Increment.
- Max price.
- Unique `(auction_id, bidder_id)`.

## 7.5. Bảng `wallets`

Lưu ví:

- User id unique.
- Balance.
- Reserved balance.

Có check balance/reserved không âm.

## 7.6. Bảng `topup_transactions`

Lưu lịch sử nạp tiền:

- User.
- Amount.
- Method.
- Status.
- Reference code.
- Transaction/complete time.

## 7.7. Bảng `wallet_transactions`

Audit log cho ví:

- TOP_UP.
- RESERVE.
- RELEASE.
- CAPTURE.
- REFUND.

## 7.8. Bảng `notifications`

Lưu thông báo:

- `user_id = 0`: thông báo chung.
- `user_id > 0`: thông báo cá nhân.

## 7.9. Bảng `homepage_announcements`

Lưu announcement trang chủ:

- Title.
- Summary.
- Details.
- Schedule text.
- Linked auction.
- Author.

## 8. Luồng nghiệp vụ chính

## 8.1. Đăng ký

```text
RegisterViewController
-> AuthController.registerGUI
-> RemoteAuthService.register
-> AUTH_REGISTER
-> AuctionRequestHandler
-> AuthController.registerGUI
-> AuthService.register
-> UserDAO.save
-> WalletService.getWallet nếu không phải admin
```

## 8.2. Đăng nhập

```text
LoginViewController
-> AuthController.login
-> RemoteAuthService.login
-> AUTH_LOGIN
-> AuctionRequestHandler
-> AuthService.login
-> AuctionSessionManager.create
-> AuthenticatedUserResponse
-> RemoteAuctionClient.setSessionToken
```

## 8.3. Tạo auction

```text
SellerDashboardViewController
-> AuctionController.createAuction
-> RemoteAuctionService.createAuction
-> AUCTION_CREATE + sessionToken
-> requireRole SELLER
-> AuctionService.createAuction
-> AuctionDAO.saveAuction
-> NotificationService.createNotification
```

## 8.4. Đặt bid

```text
BidderDashboardViewController
-> AuctionController.placeBid
-> RemoteAuctionService.placeBid
-> AUCTION_PLACE_BID + sessionToken
-> requireRole BIDDER
-> AuctionService.placeBid
-> WalletService.ensureSufficientAvailableBalanceForBid
-> applyAutoBids
-> applyAntiSniping
-> WalletService.applyReservationTransition
-> AuctionDAO.saveBid
-> AuctionDAO.updateAuctionState
-> AuctionEventBus.publish
```

## 8.5. Auto-bid

```text
Bidder tạo/sửa auto-bid
-> AUTOBID_CREATE / AUTOBID_UPDATE
-> AutobidService
-> AutoBidDAO
-> AuctionService.triggerAutoBids
```

## 8.6. Kết thúc auction

```text
AuctionScheduler mỗi 1 giây
-> AuctionService.refreshAuctionStatuses
-> Nếu now >= endTime
-> finishAuction
-> captureWinnerPayment nếu có thể
-> updateAuctionState
-> publish event
-> notification winner
```

## 8.7. Nạp tiền

```text
TopUpDialogController
-> WalletController.createTopUpRequest
-> RemoteWalletService.createTopUpRequest
-> WALLET_TOP_UP
-> WalletService.createTopUpRequest
-> save topup PENDING
-> auto update SUCCESS
-> cộng balance
-> log wallet transaction TOP_UP
```

## 8.8. Admin khóa tài khoản

```text
AdminDashboardViewController
-> AuthController.toggleUserStatus
-> AUTH_TOGGLE_STATUS
-> requireRole ADMIN
-> AuthService.toggleUserStatus
-> UserDAO.update
-> AuctionSessionManager.invalidateUser
```

## 9. Bảo mật

## 9.1. Điểm tốt

- Client không truy cập database.
- Password hash bằng PBKDF2.
- Password field trong `User` là `transient`.
- Server quản lý session token.
- Server enforce role trong `AuctionRequestHandler`.
- Socket có `ObjectInputFilter`.
- Có TLS optional.
- User bị lock/xóa bị invalidate session.

## 9.2. Rủi ro

- TCP plain là default. Nếu expose public port, nên bật TLS.
- Java serialization vẫn có rủi ro nếu filter bị cấu hình sai khi mở rộng model.
- Login gửi username/password qua socket, cần TLS khi deploy thật.
- Error message server trả về có thể lộ chi tiết nội bộ nếu exception runtime không được sanitize.

## 10. Concurrency và consistency

## 10.1. Cơ chế hiện có

- `AuctionService` lock theo auction id.
- `WalletService` lock theo user id.
- `AuctionSessionManager` dùng `ConcurrentHashMap`.
- `AuctionSocketServer` dùng bounded thread pool.
- Client dùng `UiAsync` và refresh ticket để tránh race UI.

## 10.2. Rủi ro consistency

Rủi ro lớn nhất là database transaction boundary.

Trong bid flow:

```text
reserve/release wallet
-> save bids
-> update auction state
```

Nhưng mỗi DAO tự mở connection riêng. Nếu một bước thành công và bước sau thất bại, dữ liệu có thể lệch.

Hướng cải thiện:

- Tạo transaction service dùng chung một `Connection`.
- DAO có overload nhận `Connection`.
- Gom wallet update, bid insert và auction update vào một transaction.

## 11. Test coverage

Test hiện có bảo vệ các phần quan trọng:

- Password hash.
- User serialization không lộ password.
- Auction request/response.
- Event bus.
- Socket filter.
- Socket load test.
- Session manager.
- Anti-sniping.
- Auto-payment capture.
- Concurrent bidding.
- Auction deletion refund.
- Reserved balance reconciliation.
- Settlement handler.
- Status transition.
- UI input parsing.
- FXML consistency.
- Remote client config.

Đã chạy:

```powershell
.\mvnw.cmd -q test
```

Kết quả: pass.

## 12. Điểm nóng bảo trì

## 12.1. File lớn

Những file có độ phức tạp cao:

- `BidderDashboardViewController.java`: gần 2000 dòng.
- `SellerDashboardViewController.java`: hơn 1000 dòng.
- `UiText.java`: hơn 1000 dòng.
- `AuctionService.java`: 900 dòng.
- `AdminDashboardViewController.java`: hơn 700 dòng.

Nên ưu tiên tách nhỏ nếu tiếp tục phát triển.

## 12.2. Protocol string result

Nhiều controller trả về `"SUCCESS"` hoặc message lỗi.

Rủi ro:

- Remote service phải parse string.
- Dễ sai khi đổi message.

Hướng cải thiện:

- Dùng response DTO rõ ràng hơn.
- Ví dụ `OperationResult { success, message, id }`.

## 12.3. Tiền tệ

Hiện có sự trộn lẫn:

- Auction/bid: `double`.
- Wallet: `long`.
- DB: `DECIMAL`.

Hướng cải thiện:

- Dùng `long` VND cho tất cả tiền.
- Hoặc dùng `BigDecimal` toàn bộ server/model.

## 13. Điểm mạnh của project

- Kiến trúc client-server đã tách đúng.
- Server là nơi duy nhất truy cập database.
- Session/role security được tập trung.
- Business logic đấu giá khá đầy đủ: bid, auto-bid, anti-sniping, wallet reservation, settlement.
- Test server nghiệp vụ khá tốt.
- UI có nhiều tính năng và có refresh realtime.
- Có tài liệu deploy, security và architecture.

## 14. Điểm cần cải thiện ưu tiên

### Ưu tiên 1: Transaction bid/wallet/auction

Cần gom các thao tác quan trọng vào transaction DB thật.

Tác động:

- Giảm lệch tiền.
- Giảm lệch winner/current bid.
- Tăng độ tin cậy khi server crash hoặc DB lỗi giữa flow.

### Ưu tiên 2: Chuẩn hóa tiền

Đổi tiền về `long` hoặc `BigDecimal`.

Tác động:

- Tránh sai số `double`.
- Tránh cast mất giá trị khi capture/reserve.

### Ưu tiên 3: Bật TLS khi deploy

Plain TCP chỉ nên dùng local/dev.

Tác động:

- Bảo vệ username/password khi login.
- Bảo vệ session token.

### Ưu tiên 4: Tách controller UI lớn

Tách bidder/seller dashboard thành nhiều component nhỏ.

Tác động:

- Dễ sửa UI.
- Giảm regression.
- Dễ test logic form/table riêng.

### Ưu tiên 5: Chuẩn hóa error/result protocol

Thay string `"SUCCESS"` bằng DTO.

Tác động:

- Dễ mở rộng.
- Giảm parse string.
- Lỗi hiển thị rõ hơn.

## 15. Hướng dẫn mở rộng chức năng

Khi thêm một chức năng mới qua client-server, cần sửa theo thứ tự:

1. Thêm method vào API trong `core-common/userauth/api`.
2. Thêm action vào `NetworkActions`.
3. Thêm model/DTO nếu cần.
4. Implement logic trong server service.
5. Thêm DAO query nếu cần database.
6. Thêm route trong `AuctionRequestHandler`.
7. Thêm method vào `Remote*Service`.
8. Gọi từ UI controller.
9. Thêm test cho service và request handler.

Khi thêm field vào model serialize qua socket:

1. Cập nhật model trong `core-common`.
2. Cập nhật DAO map/bind.
3. Cập nhật DB initializer/migration.
4. Build lại cả client và server cùng version.

## 16. Kết luận

Project có nền tảng tốt cho một hệ thống đấu giá client-server:

- Chia module hợp lý.
- Nghiệp vụ đấu giá khá đầy đủ.
- Có session, role, socket filter và password hashing.
- Có test cho nhiều luồng nguy hiểm.

Tuy nhiên, nếu nâng cấp lên mức ổn định hơn, cần ưu tiên:

- Transaction database cho bid/wallet/auction.
- Chuẩn hóa kiểu dữ liệu tiền.
- Bật TLS khi deploy.
- Refactor các controller UI quá lớn.
- Chuẩn hóa protocol kết quả thay vì string `"SUCCESS"`.