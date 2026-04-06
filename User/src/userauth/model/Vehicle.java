package userauth.model;

/**
 * Lớp Vehicle kế thừa AuctionItem (→ Item → Entity)
 * Đại diện cho phương tiện giao thông trong hệ thống đấu giá.
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation (private fields)
 */
public class Vehicle extends AuctionItem {
    private String make;     // Hãng xe: Toyota, Honda, ...
    private String model;    // Dòng xe: Camry, Civic, ...
    private int yearMade;    // Năm sản xuất
    private int mileage;     // Số km đã đi

    public Vehicle(int id, String name, String description, double startPrice, double currentHighestBid,
                   long startTime, long endTime, long createdAt, long updatedAt,
                   int sellerId, int winnerId, AuctionStatus status,
                   String make, String model, int yearMade, int mileage) {
        super(id, name, description, startPrice, currentHighestBid, startTime, endTime, "VEHICLE", createdAt, updatedAt, sellerId, winnerId, status);
        this.make = make;
        this.model = model;
        this.yearMade = yearMade;
        this.mileage = mileage;
    }

    public Vehicle(int id, String name, String description, double startPrice,
                   long startTime, long endTime, int sellerId,
                   String make, String model, int yearMade, int mileage) {
        super(id, name, description, startPrice, startTime, endTime, "VEHICLE", sellerId);
        this.make = make;
        this.model = model;
        this.yearMade = yearMade;
        this.mileage = mileage;
    }

    // Getter / Setter (Encapsulation)
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYearMade() { return yearMade; }
    public void setYearMade(int yearMade) { this.yearMade = yearMade; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== PHƯƠNG TIỆN GIAO THÔNG ===");
        System.out.println("ID: " + id);
        System.out.println("Tên: " + name);
        System.out.println("Mô tả: " + description);
        System.out.println("Hãng xe: " + make);
        System.out.println("Dòng xe: " + model);
        System.out.println("Năm sản xuất: " + yearMade);
        System.out.println("Số km: " + mileage);
        System.out.println("Giá khởi điểm: " + startPrice);
        System.out.println("Giá cao nhất hiện tại: " + currentHighestBid);
        System.out.println("Trạng thái đấu giá: " + getStatus());
    }
}
