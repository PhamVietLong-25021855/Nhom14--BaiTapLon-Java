# 3.2.3 Gia hạn phiên đấu giá - Anti-sniping Algorithm

## 1. Ý tưởng thuật toán

Anti-sniping là cơ chế tự động gia hạn phiên đấu giá khi có bid hợp lệ xuất hiện trong những giây cuối.

Mục đích:

- ngăn người dùng chốt giá ở vài giây cuối để người khác không kịp phản ứng;
- giữ tính công bằng cho đấu giá online;
- giảm cảm giác "mất vì độ trễ mạng" ở client JavaFX.

Nếu không có anti-sniping:

1. phiên dự kiến kết thúc lúc `20:00:00`;
2. bidder A đặt giá lúc `19:59:58`;
3. bidder B nhìn thấy giá mới nhưng không còn thời gian phản ứng;
4. hệ thống kết thúc ngay, dù bidder B hoàn toàn có thể trả cao hơn.

## 2. Rule nghiệp vụ đề xuất

Rule khuyến nghị cho đồ án:

- `X = 30 giây`: vùng kiểm tra gia hạn.
- `Y = 60 giây`: thời gian cộng thêm mỗi lần.
- chỉ bid hợp lệ mới kích hoạt gia hạn;
- chỉ gia hạn khi:
  - `end_time - X <= bid_time < end_time`
  - bid đã được server chấp nhận;
- nên có `max_extension_count`, ví dụ `5`;
- nên lưu `original_end_time` để biết deadline ban đầu;
- nên có `anti_sniping_enabled` để bật/tắt theo auction;
- nên lưu `extension_count`, `extension_threshold_seconds`, `extension_duration_seconds` trong database.

Rule thực tế đã cắm vào repo:

- timestamp dùng `long` tính bằng `milliseconds`;
- threshold/duration lưu bằng `seconds` trong DB, rồi service đổi sang `milliseconds` khi tính;
- nếu `extension_count >= max_extension_count` thì không gia hạn nữa.

## 3. Database design

Các cột cần có trong `auctions`:

- `end_time`
- `original_end_time`
- `extension_count`
- `max_extension_count`
- `anti_sniping_enabled`
- `extension_threshold_seconds`
- `extension_duration_seconds`
- `updated_at`

SQL mẫu:

```sql
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS original_end_time BIGINT DEFAULT 0;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS extension_count INT NOT NULL DEFAULT 0;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS max_extension_count INT NOT NULL DEFAULT 5;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS anti_sniping_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS extension_threshold_seconds INT NOT NULL DEFAULT 30;
ALTER TABLE auctions ADD COLUMN IF NOT EXISTS extension_duration_seconds INT NOT NULL DEFAULT 60;

UPDATE auctions
SET original_end_time = end_time
WHERE original_end_time = 0;
```

Repo đã cập nhật phần này trong `DatabaseInitializer`.

## 4. Class design

### `userauth.model.AuctionItem`

- Vai trò: model auction.
- Thuộc tính anti-sniping:
  - `originalEndTime`
  - `extensionCount`
  - `maxExtensionCount`
  - `antiSnipingEnabled`
  - `extensionThresholdSeconds`
  - `extensionDurationSeconds`

### `userauth.model.BidRequest`

- Vai trò: request DTO cho đặt giá.
- Thuộc tính chính: `auctionId`, `bidderId`, `amount`, `requestTime`.

### `userauth.model.BidResult`

- Vai trò: response DTO cho bid.
- Thuộc tính mới:
  - `previousEndTime`
  - `auctionEndTime`
  - `auctionExtended`
  - `extensionCount`

### `userauth.service.AntiSnipingService`

- Vai trò: chứa rule anti-sniping.
- Phương thức:
  - `shouldExtend(AuctionItem auction, long bidTime)`
  - `extendAuction(AuctionItem auction, long bidTime)`

### `userauth.service.AuctionService`

- Vai trò: orchestration đặt giá, concurrent bidding, auto-bid, anti-sniping.
- Tích hợp anti-sniping vào `placeBid(BidRequest request)`.

### `userauth.dao.AuctionDAO` / `AuctionDAOImpl`

- Vai trò: lưu `auction` và `bid`.
- `saveBidAndUpdateAuction(...)` dùng để persist bid và `end_time` mới trong cùng flow.

### `userauth.realtime.AuctionEventPublisher`

- Vai trò: điểm móc realtime cho Socket/Observer.
- Phương thức: `publish(AuctionUpdateEvent event)`.

### `userauth.realtime.AuctionUpdateEvent`

- Vai trò: payload gửi ra client.
- Dữ liệu:
  - `eventType`
  - `auctionId`
  - `bidId`
  - `newHighestBid`
  - `currentWinnerId`
  - `previousEndTime`
  - `newEndTime`
  - `extensionCount`

## 5. Luồng xử lý khi có bid mới

1. Bidder gửi `BidRequest`.
2. Server lock theo `auctionId`.
3. Server kiểm tra auction tồn tại, status, thời gian.
4. Server kiểm tra bid có hợp lệ không.
5. Nếu hợp lệ, gọi `AntiSnipingService.extendAuction(...)`.
6. Nếu đủ điều kiện, `end_time` tăng thêm `Y` giây và `extension_count++`.
7. Ghi bid + auction vào database.
8. Nếu có auto-bid thì tiếp tục xử lý.
9. Tạo `BidResult`.
10. Publish event:
   - luôn có `BID_ACCEPTED`;
   - nếu có gia hạn thì publish thêm `AUCTION_EXTENDED`.
11. Client JavaFX nhận event và refresh UI.

## 6. Công thức kiểm tra gia hạn

```text
remainingTime = auction.endTime - bidTime

if remainingTime >= 0
   and remainingTime <= extensionThresholdMillis
   and extensionCount < maxExtensionCount
   and antiSnipingEnabled == true
then
   newEndTime = auction.endTime + extensionDurationMillis
```

Giải thích:

- `bidTime`: thời điểm server nhận và xác nhận bid hợp lệ, đơn vị `milliseconds`;
- `remainingTime`: thời gian còn lại của phiên tại lúc bid được chấp nhận;
- `extensionThresholdMillis = extensionThresholdSeconds * 1000`;
- `extensionDurationMillis = extensionDurationSeconds * 1000`.

## 7. Code Java mẫu

### `AntiSnipingService.shouldExtend`

```java
public boolean shouldExtend(AuctionItem auction, long bidTime) {
    if (auction == null || !auction.isAntiSnipingEnabled()) {
        return false;
    }
    if (auction.getExtensionThresholdSeconds() <= 0 || auction.getExtensionDurationSeconds() <= 0) {
        return false;
    }
    if (auction.getMaxExtensionCount() >= 0
            && auction.getExtensionCount() >= auction.getMaxExtensionCount()) {
        return false;
    }

    long remainingTime = auction.getEndTime() - bidTime;
    long thresholdMillis = auction.getExtensionThresholdSeconds() * 1000L;
    return remainingTime >= 0 && remainingTime <= thresholdMillis;
}
```

### `AntiSnipingService.extendAuction`

```java
public boolean extendAuction(AuctionItem auction, long bidTime) {
    if (!shouldExtend(auction, bidTime)) {
        return false;
    }

    long extensionMillis = auction.getExtensionDurationSeconds() * 1000L;
    auction.setEndTime(auction.getEndTime() + extensionMillis);
    auction.setExtensionCount(auction.getExtensionCount() + 1);
    auction.setUpdatedAt(Math.max(auction.getUpdatedAt(), bidTime));
    return true;
}
```

### Tích hợp vào `BidService.placeBid`

```java
long now = System.currentTimeMillis();
long previousEndTime = item.getEndTime();
boolean auctionExtended = antiSnipingService.extendAuction(item, now);
BidTransaction acceptedBid = persistBid(item, bidderId, amount, now, "ACCEPTED");

BidResult result = new BidResult(
    true,
    acceptedBid.getId(),
    item.getId(),
    bidderId,
    acceptedBid.getAmount(),
    item.getCurrentHighestBid(),
    item.getWinnerId(),
    previousEndTime,
    item.getEndTime(),
    auctionExtended,
    item.getExtensionCount(),
    item.getStatus(),
    "..."
);
```

## 8. Realtime update

Khuyến nghị cho đồ án:

- nếu đang cùng JVM: `Observer Pattern`;
- nếu tách client-server thật: `Socket event`.

Event nên có:

- `BID_ACCEPTED`
- `AUCTION_EXTENDED`

JSON mẫu:

```json
{
  "eventType": "AUCTION_EXTENDED",
  "auctionId": 12,
  "bidId": 98,
  "newHighestBid": 1250000,
  "currentWinnerId": 7,
  "previousEndTime": 1746277200000,
  "newEndTime": 1746277260000,
  "extensionCount": 2
}
```

## 9. Test case cần có

Repo đã thêm test:

- `AntiSnipingServiceTest.shouldNotExtendWhenBidIsOutsideThreshold`
- `AntiSnipingServiceTest.shouldExtendWhenBidIsInsideThreshold`
- `AntiSnipingServiceTest.shouldNotExtendWhenMaxExtensionCountReached`
- `AntiSnipingServiceTest.extendAuctionUpdatesEndTimeAndCount`
- `AuctionServiceTest.bidOutsideThresholdDoesNotExtendAuctionAndPublishesBidAcceptedOnly`
- `AuctionServiceTest.invalidBidDoesNotExtendAuctionAndDoesNotPublishEvent`
- `AuctionServiceTest.maxExtensionCountPreventsFurtherExtension`

Ngoài ra nên giữ test concurrent bidding vì anti-sniping phải chạy cùng lock của auction.

## 10. Lỗi thường gặp

- Gia hạn trước khi xác nhận bid hợp lệ.
- Gia hạn cho bid bị reject.
- So sánh `seconds` với `milliseconds`.
- Chỉ đổi `end_time` trong RAM, không cập nhật DB.
- Chỉ cập nhật server, GUI không refresh.
- Không đồng bộ với concurrent bidding.
- Không có `max_extension_count`, làm auction kéo dài vô hạn.
- Không test edge case `bidTime == endTime`.

## 11. Kết luận

Phương án phù hợp nhất cho dự án Java Client-Server sinh viên:

1. lock theo `auctionId` ở server;
2. `AntiSnipingService` riêng để giữ rule sạch;
3. lưu cấu hình anti-sniping ngay trong `auctions`;
4. bid hợp lệ mới được gia hạn;
5. publish event `BID_ACCEPTED` và `AUCTION_EXTENDED` để JavaFX cập nhật realtime.

Với cách này, anti-sniping gắn chặt với concurrent bidding nhưng vẫn dễ đọc, dễ test và dễ trình bày trong báo cáo.
