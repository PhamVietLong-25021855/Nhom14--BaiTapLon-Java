# 3.2.2 Concurrent Bidding cho dự án JavaFX Auction

## 1. Vì sao phải xử lý đồng thời

Trong đấu giá, nhiều bidder có thể bấm đặt giá gần như cùng lúc vào cùng một `auctionId`. Nếu server chỉ đọc giá hiện tại rồi update mà không khóa hoặc không có transaction, dữ liệu rất dễ sai:

Ví dụ lỗi:

1. Bidder A đọc `current_highest_bid = 1_000_000`.
2. Bidder B cũng đọc `current_highest_bid = 1_000_000`.
3. Cả hai cùng gửi `1_100_000`.
4. Nếu không có concurrency control:
   - cả hai đều được xem là hợp lệ;
   - lịch sử có thể lưu 2 bid cùng hợp lệ ở cùng mức giá;
   - người thắng cuối có thể bị ghi đè bởi request về sau;
   - `current_winner_id` và `bid_history` có thể không còn nhất quán.

Đây là lỗi `lost update` điển hình.

## 2. Phương án chọn cho dự án này

### Phương án khuyến nghị cho repo hiện tại

Repo này đang chạy theo kiểu một JVM server xử lý nghiệp vụ và chỉ server mới chạm database. Với bối cảnh đó, phương án cân bằng nhất là:

1. `ReentrantLock` theo từng `auctionId` ở tầng service.
2. Transaction database khi ghi `bids` và `auctions`.

Lý do:

- Dễ code hơn `optimistic locking`.
- Dễ giải thích hơn `atomic update` thuần SQL.
- Đúng yêu cầu bài tập lớn nếu chỉ có một server process.
- Không cho hai thread trong cùng JVM cùng update một auction tại cùng thời điểm.
- Transaction đảm bảo không có trạng thái nửa chừng: insert bid thành công nhưng update auction thất bại, hoặc ngược lại.

### Khi nào dùng `SELECT ... FOR UPDATE`

Nếu muốn điểm cao hơn hoặc chuẩn bị cho nhiều server instance, nên nâng tiếp sang:

1. mở transaction;
2. `SELECT ... FOR UPDATE` trên row của auction;
3. đọc lại giá mới nhất trong transaction;
4. kiểm tra hợp lệ;
5. insert `bids`;
6. update `auctions`;
7. commit.

`ReentrantLock` chỉ bảo vệ trong một JVM. `SELECT ... FOR UPDATE` bảo vệ ở mức database row, nên an toàn hơn nếu có nhiều process.

### Đánh giá từng kỹ thuật

- `synchronized`: dễ làm, nhưng quá thô, khó lock theo từng auction, không đẹp bằng `ReentrantLock`.
- `ReentrantLock theo auctionId`: phù hợp nhất cho đồ án Java client-server một server JVM.
- `Database transaction`: bắt buộc phải có.
- `SELECT FOR UPDATE`: nên dùng nếu muốn mức chặt hơn và điểm cao hơn.
- `Optimistic locking (version)`: hợp khi xung đột ít; đấu giá thường xung đột cao nên sẽ phải retry nhiều.
- `Atomic update WHERE current_highest_bid < ?`: tốt ở mức SQL, nhưng khi còn kiểm tra status/time/history thì code sẽ khó đọc hơn.

## 3. Class design đề xuất

### `userauth.model.AuctionItem`

- Vai trò: model cho phiên đấu giá.
- Thuộc tính chính: `id`, `name`, `startPrice`, `currentHighestBid`, `winnerId`, `startTime`, `endTime`, `status`, `sellerId`.
- Dự án hiện tại đang dùng `AuctionItem` thay cho `Auction`.

### `userauth.model.BidTransaction`

- Vai trò: lịch sử bid.
- Thuộc tính chính: `id`, `auctionId`, `bidderId`, `amount`, `timestamp`, `status`.

### `userauth.model.BidRequest`

- Vai trò: DTO request gửi từ controller/API/socket vào service.
- Thuộc tính chính: `auctionId`, `bidderId`, `amount`, `requestTime`.

### `userauth.model.BidResult`

- Vai trò: DTO response trả về cho client.
- Thuộc tính chính:
  - `accepted`
  - `bidId`
  - `auctionId`
  - `bidderId`
  - `acceptedAmount`
  - `currentHighestBid`
  - `currentWinnerId`
  - `auctionEndTime`
  - `auctionStatus`
  - `message`

### `userauth.service.AuctionService`

- Vai trò: service nghiệp vụ chính cho auction.
- Phần concurrent bidding hiện đặt tại:
  - `placeBid(BidRequest request)`
  - `placeBid(int auctionId, int bidderId, double amount)` để tương thích cũ.

### `userauth.service.AuctionLockManager`

- Vai trò: quản lý lock theo `auctionId`.
- Thuộc tính chính: `ConcurrentHashMap<Integer, ReentrantLock>`.
- Phương thức chính: `getLock(int auctionId)`.

### `userauth.dao.AuctionDAO`

- Vai trò: đọc/ghi `auctions` và `bids`.
- Phương thức chính:
  - `findAuctionById`
  - `saveBidAndUpdateAuction`
  - `findBidsByAuction`
  - `markAuctionFinished`

### Exception

Repo hiện tại đã đủ tốt:

- `InvalidBidException`
- `AuctionClosedException`
- `ItemNotFoundException`
- `ValidationException`

## 4. Database design

Repo hiện tại đang có schema đủ gần yêu cầu. Nếu viết mới cho PostgreSQL/MySQL, có thể dùng:

```sql
CREATE TABLE auctions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_price DECIMAL(15,2) NOT NULL,
    current_highest_bid DECIMAL(15,2) NOT NULL,
    seller_id BIGINT NOT NULL,
    current_winner_id BIGINT NULL,
    start_time BIGINT NOT NULL,
    end_time BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE bids (
    id BIGSERIAL PRIMARY KEY,
    auction_id BIGINT NOT NULL REFERENCES auctions(id),
    bidder_id BIGINT NOT NULL,
    bid_amount DECIMAL(15,2) NOT NULL,
    bid_time BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE INDEX idx_bids_auction_time ON bids (auction_id, bid_time);
```

Ghi chú:

- Repo hiện tại dùng tên cột `winner_id`, không phải `current_winner_id`.
- `version` chỉ cần khi dùng optimistic locking.

## 5. Luồng xử lý đặt giá

1. Client JavaFX gửi `BidRequest`.
2. Server controller nhận request và gọi `AuctionService.placeBid(...)`.
3. `AuctionLockManager` lấy lock theo `auctionId`.
4. Service kiểm tra auction có tồn tại không.
5. Kiểm tra status đang `RUNNING`.
6. Kiểm tra thời gian chưa hết hạn.
7. Đọc trạng thái auction mới nhất từ DAO.
8. So sánh `newBid > currentHighestBid` và `newBid > startPrice`.
9. Nếu không hợp lệ, throw `InvalidBidException` với message rõ ràng.
10. Nếu hợp lệ, insert `BidTransaction`.
11. Update `current_highest_bid`, `winner_id`, `updated_at`, `end_time` nếu anti-sniping.
12. Commit transaction của DAO.
13. Thả lock.
14. Trả `BidResult` cho client.
15. Nếu có realtime/socket broadcast, push giá mới tới các client đang xem.

## 6. Pseudocode

```java
public BidResult placeBid(BidRequest request)
        throws ItemNotFoundException, AuctionClosedException, InvalidBidException {
    Lock lock = auctionLockManager.getLock(request.getAuctionId());
    lock.lock();
    try {
        AuctionItem item = requireAuctionForBidding(request.getAuctionId());

        if (request.getBidderId() == item.getSellerId()) {
            throw new InvalidBidException("Seller cannot bid on own auction.");
        }

        if (request.getAmount() <= item.getCurrentHighestBid()) {
            throw new InvalidBidException("Bid must be greater than current highest bid.");
        }

        ensureSufficientBalance(request.getBidderId(), request.getAmount());

        BidTransaction acceptedBid = new BidTransaction(...);
        item.setCurrentHighestBid(request.getAmount());
        item.setWinnerId(request.getBidderId());
        auctionDAO.saveBidAndUpdateAuction(acceptedBid, item); // transaction

        applyAntiSnipingIfNeeded(item, System.currentTimeMillis());

        return new BidResult(...);
    } finally {
        lock.unlock();
    }
}
```

## 7. Test đa luồng

Repo đã được bổ sung test concurrent ở `src/test/java/userauth/service/AuctionServiceTest.java`:

- `concurrentSameAmountBidsOnlyAcceptOneWinner`
  - nhiều thread cùng bid đúng một mức giá;
  - chỉ một bid được accept;
  - chỉ một winner cuối cùng.

- `concurrentIncreasingBidsPreserveHighestBidAndHistory`
  - nhiều thread cùng bid các mức giá tăng dần;
  - giá cao nhất cuối cùng phải đúng bằng mức lớn nhất;
  - `history.size()` bằng số bid được accept;
  - không có trùng `bidId`.

## 8. Lỗi thường gặp

- Chỉ validate ở client, không validate lại ở server.
- Client truy cập database trực tiếp.
- Không dùng transaction cho insert bid + update auction.
- Đọc `current_highest_bid` rồi update sau mà không có lock.
- Không lưu `bid_history`.
- Dùng một lock global cho mọi auction làm giảm throughput.
- Không xử lý hai request cùng mức giá tới gần như đồng thời.
- Insert bid lỗi nhưng vẫn update auction, hoặc ngược lại.

## 9. Kết luận cho sinh viên

Nếu mục tiêu là dễ làm nhưng vẫn đúng yêu cầu:

1. Giữ `ReentrantLock` theo `auctionId`.
2. Giữ transaction DB khi ghi bid + auction.
3. Tất cả kiểm tra hợp lệ phải nằm ở server service.
4. Trả `BidResult` để client JavaFX hoặc REST/socket có đủ dữ liệu cập nhật realtime.

Nếu muốn nâng mức hoàn thiện để lấy điểm cao hơn:

1. Chuyển `saveBidAndUpdateAuction` sang transaction có `SELECT ... FOR UPDATE`.
2. Tách riêng `BidService` khỏi `AuctionService`.
3. Thêm cơ chế broadcast realtime sau khi bid thành công.
