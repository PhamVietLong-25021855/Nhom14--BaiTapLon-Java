# Client JavaFX rieng

Client khong truy cap database truc tiep. Mac dinh `userauth.Launcher` chay remote mode va goi server qua Socket.

## Chay client tren may nguoi dung

Client mac dinh tro toi VPS `172.104.50.54:5050`:

```powershell
.\client\run-client.ps1
```

Neu doi VPS hoac dung domain server:

```powershell
.\client\run-client.ps1 -ServerHost "IP_PUBLIC_HOAC_DOMAIN" -ServerPort 5050
```

Hoac truyen VM options trong IntelliJ:

```text
-Dapp.server.host=172.104.50.54 -Dapp.server.port=5050
```

## Chay qua SSH port 22

Khong chay `AuctionServerMain` truc tiep tren port `22` vi port nay dang dung cho SSH. Neu VPS chi mo port `22`, hay chay server app tren VPS o `127.0.0.1:5050` hoac `0.0.0.0:5050`, roi dung SSH tunnel:

```powershell
.\client\run-client-via-ssh.ps1
```

Khi cua so SSH hien ra, dang nhap va giu cua so do mo. Client se ket noi toi `127.0.0.1:5050`, con duong mang that se di qua SSH port `22`.

Neu can chay lai che do local DB cu, them:

```text
-Dapp.client.mode=local
```
