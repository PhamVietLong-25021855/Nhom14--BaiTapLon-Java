# Optimization Guide

Các tối ưu chính hiện có trong project.

## Server-side filtering và sorting

Danh sách phiên đấu giá nên được lọc/sắp xếp ở server khi dữ liệu lớn, tránh kéo toàn bộ dữ liệu về client rồi mới xử lý.

## AuctionCache

`AuctionCache` lưu tạm dữ liệu ít thay đổi trong vài giây để giảm truy vấn database.

- Danh sách phiên đang chạy/open: TTL ngắn.
- Phiên đã kết thúc: TTL dài hơn.
- Khi bid hoặc status thay đổi, cache liên quan đến auction đó bị invalidate.

## Image optimization

Ảnh sản phẩm được xử lý qua tiện ích nén/giảm kích thước để hạn chế dữ liệu truyền qua socket và lưu database.

## Pagination without images

Danh sách có thể tải metadata trước, chỉ tải ảnh khi cần hiển thị chi tiết/card.

## Transactional bid updates

Khi đặt giá:

1. Kiểm tra trạng thái phiên.
2. Kiểm tra số dư ví.
3. Giữ tiền bidder mới.
4. Giải phóng tiền bidder cũ nếu bị vượt giá.
5. Lưu bid và phát event.

Các bước này cần giữ nhất quán để tránh lệch `balance` và `reservedBalance`.

## Database indexes

File `database_indexes.sql` chứa index hỗ trợ các truy vấn thường dùng như danh sách auction, bid history và wallet transaction.

## Theo dõi lỗi hiệu năng

- Xem log server khi request chậm.
- Kiểm tra query thường xuyên dùng có index chưa.
- Theo dõi số lượng ảnh lớn hoặc response quá nặng.
- Chạy `mvn test` sau khi sửa service/cache.
