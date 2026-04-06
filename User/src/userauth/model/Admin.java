package userauth.model;

/**
 * Lớp Admin kế thừa User (→ Entity).
 * Thể hiện: Inheritance, Polymorphism (override printInfo), Encapsulation
 */
public class Admin extends User {
    private String department; // Phòng ban quản lý

    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = "SYSTEM"; // Mặc định
    }

    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt, String department) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = department;
    }

    // Getter / Setter
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    // Polymorphism: override printInfo()
    @Override
    public void printInfo() {
        System.out.println("=== THÔNG TIN ADMIN ===");
        System.out.println("ID: " + getId());
        System.out.println("Username: " + getUsername());
        System.out.println("Họ tên: " + getFullName());
        System.out.println("Email: " + getEmail());
        System.out.println("Phòng ban: " + department);
        System.out.println("Vai trò: " + getRoleName());
        System.out.println("Trạng thái: " + getStatus());
    }
}
