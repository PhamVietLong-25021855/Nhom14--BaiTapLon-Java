# Các design pattern đang dùng

## Singleton

`userauth.event.AuctionEventBus` được triển khai theo Singleton thông qua `getInstance()`.

- Ứng dụng dùng chung một event hub cho toàn bộ cập nhật đấu giá.
- Controller đăng ký lắng nghe khi màn hình được kích hoạt và hủy đăng ký khi màn hình bị tắt.

## Factory Method

`userauth.service.AuctionSettlementHandlerFactory` tạo handler xử lý trạng thái cuối của một phiên đấu giá đã kết thúc.

- `PAID` dùng handler riêng cho trạng thái đã thanh toán.
- `CANCELED` dùng handler riêng với logic validate và cập nhật trạng thái khác.
- `AuctionService` ủy quyền luồng `FINISHED -> PAID/CANCELED` cho handler do factory tạo ra, thay vì viết cứng nhiều nhánh xử lý trong cùng một hàm.

## Observer

Luồng Observer xoay quanh các lớp:

- `AuctionEventBus`
- `AuctionEventListener`
- `AuctionEvent`

Các observer hiện tại:

- `BidderDashboardViewController`
- `SellerDashboardViewController`

Các publisher hiện tại:

- `AuctionService` phát sự kiện khi có bid mới, khi anti-sniping kéo dài thời gian, khi trạng thái phiên thay đổi và khi phiên đã kết thúc được xử lý thanh toán/hủy.
