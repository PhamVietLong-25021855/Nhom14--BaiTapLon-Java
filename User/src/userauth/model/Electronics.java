package userauth.model;

/**
 * Lớp Electronics kế thừa AuctionItem (→ Item → Entity)
 * Đại diện cho sản phẩm điện tử trong hệ thống đấu giá.
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation (private fields)
 */
public class Electronics extends AuctionItem {
    private String brand;       // Thương hiệu (Samsung, Apple, ...)
    private int warrantyMonths; // Bảo hành (tháng)
    private String condition;   // Tình trạng: NEW, USED, REFURBISHED

    public Electronics(int id, String name, String description, double startPrice, double currentHighestBid,
                       long startTime, long endTime, long createdAt, long updatedAt,
                       int sellerId, int winnerId, AuctionStatus status,
                       String brand, int warrantyMonths, String condition) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, "ELECTRONICS", createdAt, updatedAt, sellerId, winnerId, status);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
    }

    public Electronics(int id, String name, String description, double startPrice,
                       long startTime, long endTime, int sellerId,
                       String brand, int warrantyMonths, String condition) {
        super(id, name, description, startPrice, startTime, endTime, "ELECTRONICS", sellerId);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
        this.condition = condition;
    }

    // Getter / Setter (Encapsulation)
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getWarrantyMonths() { return warrantyMonths; }
    public void setWarrantyMonths(int warrantyMonths) { this.warrantyMonths = warrantyMonths; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== SẢN PHẨM ĐIỆN TỬ ===");
        System.out.println("ID: " + id);
        System.out.println("Tên: " + name);
        System.out.println("Mô tả: " + description);
        System.out.println("Thương hiệu: " + brand);
        System.out.println("Bảo hành: " + warrantyMonths + " tháng");
        System.out.println("Tình trạng: " + condition);
        System.out.println("Giá khởi điểm: " + startPrice);
        System.out.println("Giá cao nhất hiện tại: " + currentHighestBid);
        System.out.println("Trạng thái đấu giá: " + getStatus());
    }
}
