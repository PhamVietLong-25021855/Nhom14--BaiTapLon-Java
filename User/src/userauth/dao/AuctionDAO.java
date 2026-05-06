package userauth.dao;

import userauth.model.AuctionItem;
import userauth.model.BidTransaction;
import java.util.List;

// Ghi chu file: File DAO; dinh nghia hoac trien khai cac thao tac doc ghi du lieu voi database.
// Khai bao giao dien AuctionDAO; phu trach hop dong hoac truy cap du lieu cho database.
public interface AuctionDAO {
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save auction.
    void saveAuction(AuctionItem item);
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac update auction.
    void updateAuction(AuctionItem item);
    // Phuong thuc: huy, xoa, dong hoac don trang thai cho thao tac delete auction.
    void deleteAuction(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find auction by id.
    AuctionItem findAuctionById(int id);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all auctions.
    List<AuctionItem> findAllAuctions();
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save bid.
    void saveBid(BidTransaction bid);
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac save bid and update auction.
    void saveBidAndUpdateAuction(BidTransaction bid, AuctionItem item);
    // Phuong thuc: lay hoac doc du lieu cho thao tac find all bids.
    List<BidTransaction> findAllBids();
    // Phuong thuc: lay hoac doc du lieu cho thao tac find bids by auction.
    List<BidTransaction> findBidsByAuction(int auctionId);
}
