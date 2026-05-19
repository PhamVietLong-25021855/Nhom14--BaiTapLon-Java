# Nâng cấp Client-Server

## Mục tiêu

- Tách tiến trình chạy thành `Server` và `Client`.
- Chỉ server được mở kết nối JDBC tới database.
- Client JavaFX chạy ở `remote mode` và gọi server qua Socket.
- Bổ sung kiểm thử JUnit cho logic đăng ký, đăng nhập và đấu giá đồng thời.

## Luồng chạy mới

```text
JavaFX Client
   |
   | Socket request/response
   v
AuctionServerMain
   |
   v
Controller -> Service -> DAO -> Akamai MySQL
```

## Entry point chính

- `userauth.server.AuctionServerMain`: chạy server trên VPS hoặc máy host.
- `userauth.Launcher`: chạy JavaFX client. Thêm `-Dapp.client.mode=remote` để bật chế độ client-server.
- `userauth.client.remote.*`: các service remote giúp client không truy cập database trực tiếp.
- `userauth.network.*`: request/response dùng cho giao tiếp socket.

## Kiểm thử đã thêm

- `AuthServiceTest`: kiểm thử đăng ký, đăng nhập, trùng username và sai mật khẩu.
- `AuctionServiceTest`: kiểm thử bid không hợp lệ và nhiều bidder đặt giá đồng thời.
- Các test service khác kiểm tra lifecycle phiên đấu giá, auto-bid, settlement và validation.
