# Tach client va server thanh 2 bo deploy rieng

Script `scripts/split-client-server.ps1` se tao:

- `dist/server`: dung de copy len may chay server.
- `dist/client`: dung de copy sang may nguoi dung.

## 1) Tao goi tach rieng

```powershell
.\scripts\split-client-server.ps1
```

## 2) Chay server (tren may host)

Vao thu muc `dist/server`:

```powershell
.\run-server.ps1 -DbPassword "mat_khau_db" -ServerPort 5050
```

## 3) Chay client (tren may khac)

Vao thu muc `dist/client`:

```powershell
.\run-client.ps1 -ServerHost "IP_PUBLIC_CUA_SERVER" -ServerPort 5050
```

## Ghi chu

- Neu client o mang khac, can mo firewall/port forwarding TCP 5050 tren may server.
- Khong commit mat khau that vao source code.
