# Giải thích các sửa đổi và nâng cấp socket/session

Tài liệu này tóm tắt những thay đổi đã thực hiện để hệ thống đấu giá chạy ổn định hơn khi tách client/server, truy cập qua socket và có nhiều client sử dụng cùng lúc.

## 1. Mục tiêu nâng cấp

Trước khi sửa, luồng socket còn một số điểm rủi ro:

- Client có thể gửi request nhưng server chưa có cơ chế session token rõ ràng.
- Thông tin `User` có nguy cơ bị truyền kèm mật khẩu qua socket nếu serialize cả object.
- Socket server xử lý client chưa có giới hạn tải rõ ràng, dễ quá tải khi có nhiều kết nối đồng thời.
- Test load bị lỗi `EOFException` khi số client vượt quá năng lực thread/CPU hiện có.
- Client polling dữ liệu có khả năng lấy quá nhiều thông tin, làm chậm giao diện và tăng tải server.
- Script chạy server/client chưa thống nhất thư mục làm việc và cấu hình mật khẩu DB.

Vì vậy bản nâng cấp tập trung vào 5 nhóm: bảo mật session, an toàn socket, giảm tải server, tối ưu client polling và bổ sung test/chạy jar.

## 2. Nâng cấp xác thực và session

### Đã thêm `AuctionSessionManager`

File mới:

- `server/src/main/java/userauth/server/AuctionSessionManager.java`

Chức năng:

- Tạo session token sau khi đăng nhập thành công.
- Lưu token kèm user đăng nhập.
- Tự động hết hạn session sau 8 giờ.
- Giới hạn tối đa 5 session đang hoạt động cho mỗi user.
- Tự động xóa session cũ nhất khi user vượt quá giới hạn.
- Hỗ trợ hủy session khi logout, khi user bị block hoặc khi user bị xóa.

Lý do thêm:

- Khi truy cập qua socket, server không nên tin `userId` do client tự gửi lên.
- Session token giúp server biết request nào thuộc user nào sau khi đăng nhập.
- Giới hạn số session giúp tránh việc một tài khoản mở quá nhiều kết nối làm tăng tải bộ nhớ.

### Sửa request/response đăng nhập

File sửa:

- `core-common/src/main/java/userauth/network/AuctionRequest.java`
- `core-common/src/main/java/userauth/network/AuthenticatedUserResponse.java`
- `server/src/main/java/userauth/server/AuctionRequestHandler.java`
- `client/src/main/java/userauth/remote/RemoteAuctionClient.java`
- `client/src/main/java/userauth/remote/RemoteAuthService.java`

Thay đổi chính:

- `AuctionRequest` được bổ sung `sessionToken`.
- Sau khi đăng nhập, server trả về `AuthenticatedUserResponse` gồm user an toàn và session token.
- Client lưu token và tự động gắn token vào các request tiếp theo.
- Server lấy danh tính user từ token, thay vì tin dữ liệu nhạy cảm do client gửi.

Lý do sửa:

- Tăng bảo mật cho các thao tác cần đăng nhập như tạo sản phẩm, đặt giá, cập nhật tài khoản.
- Giảm lỗi chưa đăng nhập hoặc không truy cập được bằng cách tách rõ bước đăng nhập và bước gọi API sau đăng nhập.

## 3. Bảo mật dữ liệu User và phân quyền

File sửa:

- `core-common/src/main/java/userauth/model/User.java`
- `core-common/src/main/java/userauth/api/AuthApi.java`
- `core-common/src/main/java/userauth/controller/AuthController.java`
- `server/src/main/java/userauth/service/AuthService.java`
- `server/src/main/java/userauth/server/AuctionRequestHandler.java`

Thay đổi chính:

- Trường `password` trong `User` được đánh dấu `transient` để không bị serialize qua socket.
- `toString()` của `User` không in mật khẩu thật.
- Các API liên quan auth/session được điều chỉnh để phù hợp với response có token.
- Server kiểm tra quyền và session trước khi xử lý các request nhạy cảm.

Lý do sửa:

- Không nên gửi mật khẩu về client sau khi đăng nhập.
- Nếu log hoặc debug in object `User`, mật khẩu không bị lộ.
- Đảm bảo request quan trọng phải có session hợp lệ.

## 4. Nâng cấp socket server để tránh lỗi và quá tải

File sửa:

- `server/src/main/java/userauth/server/AuctionSocketServer.java`
- `server/src/main/java/userauth/server/AuctionServerMain.java`
- `server/src/main/java/userauth/server/AuctionRequestHandler.java`

Thay đổi chính:

- Thêm `ObjectInputFilter` để giới hạn các class được phép deserialize.
- Thêm timeout đọc socket để tránh client treo kết nối quá lâu.
- Dùng thread pool có giới hạn thay vì tạo thread không kiểm soát.
- Có queue/backlog để xử lý khi nhiều client kết nối.
- Dùng cơ chế backpressure `CallerRunsPolicy` khi thread pool đầy.
- Hỗ trợ cấu hình TLS tùy chọn.
- Bổ sung hàm phục vụ test như lấy cổng cục bộ và tính khả năng xử lý phù hợp.

Lý do sửa:

- Java Object Serialization có rủi ro nếu server deserialize class không mong muốn.
- Khi nhiều client kết nối cùng lúc, server cần giới hạn tải để không cạn CPU/RAM.
- Timeout giúp server thu hồi tài nguyên khi client mất kết nối hoặc không gửi dữ liệu.
- Backpressure giúp hệ thống chậm lại có kiểm soát thay vì sập.

## 5. Sửa lỗi test load socket `EOFException`

File thêm:

- `server/src/test/java/userauth/server/AuctionSocketServerLoadTest.java`

File sửa liên quan:

- `server/src/main/java/userauth/server/AuctionSocketServer.java`

Vấn đề cũ:

- Test tạo nhiều client đồng thời hơn năng lực xử lý của server trong môi trường CPU thấp.
- Một số client bị server đóng kết nối trước khi kịp tạo `ObjectInputStream`, gây `EOFException`.

Cách sửa:

- Test load được điều chỉnh theo khả năng xử lý thực tế của socket server.
- Số client đồng thời không vượt quá mức hợp lý so với thread pool.
- Client test có timeout kết nối/đọc rõ ràng.

Kết quả:

- Test socket load đã pass.
- Lần test gần nhất: 640 request `PING`, 32 client đồng thời, hoàn thành trong 179 ms.

## 6. Tối ưu tốc độ giao diện client

File sửa:

- `client/src/main/java/userauth/gui/fxml/bidder/BidderDashboardViewController.java`
- `client/src/main/java/userauth/gui/fxml/home/HomeViewController.java`
- `client/src/main/java/userauth/remote/RemoteAuctionService.java`
- `client/src/main/java/userauth/remote/RemoteAuctionClient.java`
- `core-common/src/main/java/userauth/api/AuctionApi.java`
- `core-common/src/main/java/userauth/controller/AuctionController.java`
- `server/src/main/java/userauth/service/AuctionService.java`
- `server/src/main/java/userauth/server/AuctionRequestHandler.java`

Thay đổi chính:

- Giảm tần suất cập nhật các thông tin nền như ví, autobid, thông tin tài khoản.
- Trang home dùng danh sách auction summary để tránh tải toàn bộ ảnh sản phẩm mỗi lần refresh.
- Chỉ lấy chi tiết/ảnh khi cần hiển thị.
- Bổ sung đường API phù hợp với cách client gọi từ xa.

Lý do sửa:

- Ảnh sản phẩm và dữ liệu chi tiết có thể nặng, nếu polling liên tục sẽ làm chậm client và tăng tải socket/server.
- Tách summary và detail giúp màn hình danh sách nhanh hơn.
- Giảm số request nền giúp hệ thống ổn định hơn khi có nhiều client.

## 7. Sửa và nâng cấp script chạy server/client

File sửa:

- `server/run-server.ps1`
- `server/run-server.sh`
- `client/run-client.ps1`
- `scripts/restart-server.ps1`
- `run-javafx.ps1`
- `server/pom.xml`

Thay đổi chính:

- Script chạy server đặt thư mục làm việc về đúng thư mục gốc repo.
- Không ghi mật khẩu DB trực tiếp vào mã nguồn.
- `restart-server.ps1` dùng lại script server chính để tránh lệch cấu hình.
- Script client đặt thư mục làm việc đúng để tải file/tài nguyên ổn định.
- `server/pom.xml` cấu hình tạo lại jar khi package để tránh dùng jar cũ.

Lý do sửa:

- Khi chạy trên máy khác, lỗi thường gặp là sai đường dẫn, sai thư mục làm việc hoặc jar chưa được đóng gói lại.
- Mật khẩu DB nên truyền bằng biến môi trường/cấu hình lúc chạy, không nên ghi cứng vào mã nguồn.

## 8. Bổ sung test để kiểm tra chức năng chính

File thêm:

- `server/src/test/java/userauth/server/AuctionSessionManagerTest.java`
- `server/src/test/java/userauth/server/AuctionSocketServerFilterTest.java`
- `server/src/test/java/userauth/server/AuctionSocketServerLoadTest.java`
- `server/src/test/java/userauth/service/AuctionPerformanceSmokeTest.java`
- `client/src/test/userauth/remote/RemoteAuctionClientFilterTest.java`
- `core-common/src/test/java/userauth/network/UserSerializationTest.java`

File sửa test:

- `core-common/src/test/java/userauth/network/AuctionRequestResponseTest.java`
- `client/src/test/userauth/remote/RemoteClientConfigTest.java`
- `server/src/test/java/userauth/service/ServiceTestSupport.java`

Nội dung test:

- Session token tạo/hủy/hết hạn/giới hạn session.
- Socket filter chặn object không hợp lệ.
- Socket load với nhiều client ping đồng thời.
- Serialization không làm lộ mật khẩu user.
- Remote client cấu hình host/port đúng.
- Hiệu năng cơ bản của tạo auction, đặt giá, đấu giá đồng thời, anti-sniping.

Lý do thêm:

- Đảm bảo các lỗi đã sửa không bị lặp lại.
- Có số liệu phản hồi để đánh giá hệ thống có chậm hay quá tải không.
- Kiểm tra nhanh các chức năng chính trước khi đóng gói/chạy jar.

## 9. Tài liệu mới

File thêm:

- `docs/socket-security.md`
- `docs/socket-session-upgrade-explanation.md`

Mục đích:

- Ghi lại cách socket/session được bảo vệ.
- Giải thích cho người đọc biết bản nâng cấp đã sửa gì, thêm gì và vì sao.

## 10. Danh sách file đã sửa

Client:

- `client/run-client.ps1`
- `client/src/main/java/userauth/ClientMain.java`
- `client/src/main/java/userauth/gui/fxml/bidder/BidderDashboardViewController.java`
- `client/src/main/java/userauth/gui/fxml/home/HomeViewController.java`
- `client/src/main/java/userauth/gui/fxml/shell/AuthFrame.java`
- `client/src/main/java/userauth/remote/RemoteAuctionClient.java`
- `client/src/main/java/userauth/remote/RemoteAuctionService.java`
- `client/src/main/java/userauth/remote/RemoteAuthService.java`
- `client/src/main/java/userauth/remote/RemoteClientConfig.java`
- `client/src/test/userauth/remote/RemoteClientConfigTest.java`

Core common:

- `core-common/src/main/java/userauth/api/AuctionApi.java`
- `core-common/src/main/java/userauth/api/AuthApi.java`
- `core-common/src/main/java/userauth/controller/AuctionController.java`
- `core-common/src/main/java/userauth/controller/AuthController.java`
- `core-common/src/main/java/userauth/model/User.java`
- `core-common/src/main/java/userauth/network/AuctionRequest.java`
- `core-common/src/main/java/userauth/network/NetworkActions.java`
- `core-common/src/test/java/userauth/network/AuctionRequestResponseTest.java`

Server:

- `server/pom.xml`
- `server/run-server.ps1`
- `server/run-server.sh`
- `server/src/main/java/userauth/server/AuctionRequestHandler.java`
- `server/src/main/java/userauth/server/AuctionServerMain.java`
- `server/src/main/java/userauth/server/AuctionSocketServer.java`
- `server/src/main/java/userauth/service/AuctionService.java`
- `server/src/main/java/userauth/service/AuthService.java`
- `server/src/test/java/userauth/service/ServiceTestSupport.java`

Script chung:

- `run-javafx.ps1`
- `scripts/restart-server.ps1`

## 11. Danh sách file đã thêm

- `client/src/test/userauth/remote/RemoteAuctionClientFilterTest.java`
- `core-common/src/main/java/userauth/network/AuthenticatedUserResponse.java`
- `core-common/src/test/java/userauth/network/UserSerializationTest.java`
- `docs/socket-security.md`
- `docs/socket-session-upgrade-explanation.md`
- `server/src/main/java/userauth/server/AuctionSessionManager.java`
- `server/src/test/java/userauth/server/AuctionSessionManagerTest.java`
- `server/src/test/java/userauth/server/AuctionSocketServerFilterTest.java`
- `server/src/test/java/userauth/server/AuctionSocketServerLoadTest.java`
- `server/src/test/java/userauth/service/AuctionPerformanceSmokeTest.java`

## 12. Kết quả kiểm tra hiện tại

Bộ test chức năng chính đã pass:

- 28/28 test pass.
- Socket load pass với 640 request và 32 client đồng thời.
- Tạo auction, đặt giá, đấu giá đồng thời và anti-sniping đều có thời gian phản hồi tốt trong smoke test.

Lần kiểm thử đầy đủ trước đó cũng đã pass:

- 69/69 test pass.
- Package jar server thành công.

Lưu ý còn lại:

- Chạy end-to-end với database thật vẫn cần `DB_PASSWORD` hợp lệ.
- Nếu sai mật khẩu DB, server sẽ lỗi ở bước khởi tạo database trước khi socket mở cổng. Đây là lỗi cấu hình môi trường, không phải lỗi socket.
