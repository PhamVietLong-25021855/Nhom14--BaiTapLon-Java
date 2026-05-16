package uet.auctionsystem.event;
@FunctionalInterface

// Ghi chu file: File ho tro co che su kien; dung de phat va nhan cap nhat trang thai dau gia.
// Khai bao giao dien AuctionEventListener; phuc vu co che observer cho cac cap nhat dau gia.
public interface AuctionEventListener {
    // Phuong thuc: dinh nghia hop dong xu ly cho thao tac on auction event.
    void onAuctionEvent(AuctionEvent event);
}
