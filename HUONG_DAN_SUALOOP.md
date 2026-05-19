# Hướng Dẫn Sửa Lỗi: Không Thể Đặt Giá Sau Khi Nạp Tiền

## Vấn Đề
Người dùng nạp tiền thành công nhưng không đặt được giá.

## Nguyên Nhân
1. **Không xử lý lỗi tốt** — Lỗi không đủ tiền không được báo rõ
2. **Không ghi log đủ** — Không thể debug quá trình kiểm tra số dư
3. **Silent failures** — Lỗi không có thông báo chi tiết

## Các Thay Đổi

### 1. AuctionService.java
- Thêm try-catch chuyển `ValidationException` → `InvalidBidException`
- Báo lỗi rõ ràng cho người dùng

### 2. WalletService.java
Thêm log `[Wallet]` vào 5 hàm:
- `createTopUpRequest()` — số dư trước/sau nạp tiền
- `ensureSufficientAvailableBalanceForBid()` — kiểm tra số dư
- `reserveAdditionalFunds()` — kết quả khóa tiền
- `releaseReservedFundsInternal()` — kết quả giải phóng tiền
- `applyReservationTransition()` — chuyển tiền giữa người đặt

## Test Nhanh (5 phút)

### Bước 1: Biên dịch
```bash
cd z:\Nhom14--BaiTapLon-Java-Kiet-New
mvn clean -DskipTests package
```

### Bước 2: Chạy server
```bash
cd server
java -Dapp.server.port=5050 -cp "target/classes:target/lib/*" userauth.server.AuctionServerMain
```
Giữ terminal mở để xem log `[Wallet]`.

### Bước 3: Test
1. Tạo tài khoản mới
2. Nạp 1,000,000 VND
3. Đặt giá 500,000 VND trên auction đang chạy

## Kiểm Tra Kết Quả

### Log mong đợi
**Sau khi nạp tiền:**
```
[Wallet] Top-up completed - Balance=1,000,000, Available=1,000,000
```

**Sau khi đặt giá:**
```
[Wallet] Successfully reserved 500,000 - New reserved: 500,000
```

### Bảng trạng thái ví

| Thao tác | Số dư | Có sẵn | Đã khóa |
|----------|--------|---------|---------|
| Nạp 1M | 1,000,000 | 1,000,000 | 0 |
| Đặt 500K | 1,000,000 | 500,000 | 500,000 |
| Đặt thêm 300K | 1,000,000 | 200,000 | 800,000 |

## Xử Lý Lỗi

### Lỗi: "Insufficient available wallet balance"
1. Kiểm tra log `[Wallet]` trên server
2. Xem số dư có bằng số tiền nạp không
3. Thử khởi động lại server

### Lỗi: Không thấy log [Wallet]
1. Kiểm tra terminal server có mở không
2. Kiểm tra không redirect output
3. Kiểm tra server có đang chạy không

### Kiểm tra database
```sql
SELECT user_id, balance, reserved_balance FROM wallets WHERE user_id = X;
```
