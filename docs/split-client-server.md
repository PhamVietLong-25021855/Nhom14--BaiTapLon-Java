# Tách Client Và Server

Script `scripts/split-client-server.ps1` tạo 2 gói runtime riêng để deploy client và server trên 2 máy khác nhau.

## Chạy script

```powershell
.\scripts\split-client-server.ps1
```

Kết quả:

```text
dist/server
dist/client
```

## Gói server

Chứa:

- Model/API/network dùng chung.
- Controller/service/DAO/database.
- MySQL driver.
- Script chạy server.

## Gói client

Chứa:

- Model/API/network dùng chung.
- JavaFX UI, FXML/CSS.
- Remote service.
- JavaFX runtime dependency.

Không chứa DAO, database config, server service hoặc MySQL driver.

## Chạy sau khi tách

Server:

```powershell
cd dist\server
.\run-server.ps1 -DbPassword "mat_khau_database" -ServerPort 5050
```

Client:

```powershell
cd dist\client
.\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```
