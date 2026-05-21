# Nâng Cấp Client-Server

Project đã được tách thành 3 module Maven:

| Module | Vai trò |
| --- | --- |
| `core-common` | Model, API interface, request/response, util dùng chung |
| `client` | JavaFX UI và remote service gọi server |
| `server` | Socket server, service, DAO và database |

## Luồng chạy

```text
Client JavaFX -> Remote service -> Socket -> Server handler -> Service -> DAO -> MySQL
```

Client không truy cập database trực tiếp. Toàn bộ quyền truy cập MySQL nằm ở server.

## Entry point

- Client: `userauth.ClientLauncher`
- Server: `userauth.server.AuctionServerMain`

## Kiểm thử liên quan

- FXML/resource consistency.
- Remote client config.
- Network request/response.
- Event bus.
- Wallet, cache và settlement logic ở server.
