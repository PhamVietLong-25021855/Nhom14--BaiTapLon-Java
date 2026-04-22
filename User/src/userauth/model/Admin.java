package userauth.model;

public class Admin extends User {
    private String department;

    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = "SYSTEM";
    }

    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt, String department) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
