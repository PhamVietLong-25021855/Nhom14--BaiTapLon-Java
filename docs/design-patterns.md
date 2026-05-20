# Design Patterns

Project dùng một số pattern chính để tách giao diện, nghiệp vụ và hạ tầng.

## Singleton

Dùng cho thành phần dùng chung toàn ứng dụng, ví dụ `AuctionEventBus`.

Mục đích: có một kênh phát event thống nhất giữa các phần cần theo dõi thay đổi phiên đấu giá.

## Factory Method

Dùng trong `AuctionSettlementHandlerFactory`.

Mục đích: chọn handler phù hợp khi seller/admin xác nhận kết quả phiên đấu giá, ví dụ `PAID` hoặc `CANCELED`.

## Observer

Dùng qua `AuctionEventBus` và `AuctionEventListener`.

Mục đích: service phát event khi bid/status thay đổi, UI hoặc thành phần khác có thể subscribe để cập nhật.

## Layered Architecture

Server được chia theo lớp:

```text
RequestHandler -> Controller -> Service -> DAO -> Database
```

Client được chia theo lớp:

```text
FXML Controller -> Remote Service -> Socket Client
```
