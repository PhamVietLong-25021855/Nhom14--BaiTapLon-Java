package uet.auctionsystem.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Admin; mo ta cau truc du lieu cua doi tuong nghiep vu.
public class Admin extends User {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho department.
    private String department;
    // Ham tao: khoi tao doi tuong Admin voi cac phu thuoc can thiet.
    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = "SYSTEM";
    }
    // Ham tao: khoi tao doi tuong Admin voi cac phu thuoc can thiet.
    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt, String department) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = department;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get department.
    public String getDepartment() {
        return department;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set department.
    public void setDepartment(String department) {
        this.department = department;
    }
}
