# Nâng cấp Client-Server

Project đã được tách thành 3 module Maven:

| Module | Vai trò |
| --- | --- |
| `core-common` | Model, giao diện API, request/response và tiện ích dùng chung |
| `client` | Giao diện JavaFX và remote service gọi server |
| `server` | Socket server, service, DAO và database |

## Luồng xử lý

```text
Client JavaFX -> Remote service -> Socket -> Server handler -> Service -> DAO -> MySQL
```

Client không truy cập database trực tiếp. Toàn bộ quyền truy cập MySQL nằm ở server.

## Điểm khởi chạy

- Client: `userauth.ClientLauncher`
- Server: `userauth.server.AuctionServerMain`

## Kiểm thử liên quan

- Tính nhất quán của FXML/tài nguyên.
- Cấu hình remote client.
- Request/response qua network.
- Event bus.
- Ví, cache và logic quyết toán ở server.

## Tài liệu chi tiết hơn

Xem thêm [project-architecture-and-flow.md](project-architecture-and-flow.md) để hiểu chi tiết cấu trúc thư mục, mối liên hệ giữa client-server-database-socket, luồng chạy chính và vai trò của các file `.jar`.
