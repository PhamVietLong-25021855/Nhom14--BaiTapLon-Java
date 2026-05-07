# NĂ¢ng cáº¥p Client-Server

## Má»¥c tiĂªu

- TĂ¡ch tiáº¿n trĂ¬nh cháº¡y thĂ nh `Server` vĂ  `Client`.
- Chá»‰ server Ä‘Æ°á»£c má»Ÿ káº¿t ná»‘i JDBC tá»›i database.
- Client JavaFX cháº¡y `remote mode` vĂ  gá»i server qua Socket.
- Bá»• sung test JUnit cho logic Ä‘Äƒng kĂ½/Ä‘Äƒng nháº­p vĂ  logic Ä‘áº¥u giĂ¡ Ä‘á»“ng thá»i.

## Luá»“ng cháº¡y má»›i

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

## CĂ¡c entry point chĂ­nh

- `userauth.server.AuctionServerMain`: cháº¡y server trĂªn VPS.
- `userauth.Launcher`: cháº¡y JavaFX client; thĂªm `-Dapp.client.mode=remote` Ä‘á»ƒ báº­t cháº¿ Ä‘á»™ client-server.
- `userauth.client.remote.*`: cĂ¡c service remote Ä‘á»ƒ client khĂ´ng truy cáº­p database.
- `userauth.network.*`: request/response dĂ¹ng cho giao tiáº¿p socket.

## Test Ä‘Ă£ thĂªm

- `AuthServiceTest`: kiá»ƒm thá»­ Ä‘Äƒng kĂ½, Ä‘Äƒng nháº­p, trĂ¹ng username, sai máº­t kháº©u.
- `AuctionServiceTest`: kiá»ƒm thá»­ bid khĂ´ng há»£p lá»‡ vĂ  nhiá»u bidder Ä‘áº·t giĂ¡ Ä‘á»“ng thá»i.

