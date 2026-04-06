package userauth.model;

/**
 * Lớp Art kế thừa AuctionItem (→ Item → Entity)
 * Đại diện cho tác phẩm nghệ thuật trong hệ thống đấu giá.
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation (private fields)
 */
public class Art extends AuctionItem {
    private String artist;  // Tên nghệ sĩ / tác giả
    private int year;       // Năm sáng tác
    private String medium;  // Chất liệu: Oil, Watercolor, Digital, ...

    public Art(int id, String name, String description, double startPrice, double currentHighestBid,
               long startTime, long endTime, long createdAt, long updatedAt,
               int sellerId, int winnerId, AuctionStatus status,
               String artist, int year, String medium) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, "ART", createdAt, updatedAt, sellerId, winnerId, status);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    public Art(int id, String name, String description, double startPrice,
               long startTime, long endTime, int sellerId,
               String artist, int year, String medium) {
        super(id, name, description, startPrice, startTime, endTime, "ART", sellerId);
        this.artist = artist;
        this.year = year;
        this.medium = medium;
    }

    // Getter / Setter (Encapsulation)
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== TÁC PHẨM NGHỆ THUẬT ===");
        System.out.println("ID: " + id);
        System.out.println("Tên: " + name);
        System.out.println("Mô tả: " + description);
        System.out.println("Nghệ sĩ: " + artist);
        System.out.println("Năm sáng tác: " + year);
        System.out.println("Chất liệu: " + medium);
        System.out.println("Giá khởi điểm: " + startPrice);
        System.out.println("Giá cao nhất hiện tại: " + currentHighestBid);
        System.out.println("Trạng thái đấu giá: " + getStatus());
    }
}
