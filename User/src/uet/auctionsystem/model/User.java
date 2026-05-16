package uet.auctionsystem.model;

import uet.auctionsystem.util.PasswordUtil;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop User; mo ta cau truc du lieu cua doi tuong nghiep vu.
public abstract class User extends Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho username.
    private String username;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho password.
    private String password;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho full name.
    private String fullName;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho email.
    private String email;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho role.
    private Role role;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho status.
    private String status; // ACTIVE, BLOCKED
    // Thuoc tinh: luu trang thai hoac du lieu tam cho created at.
    private long createdAt;
    // Thuoc tinh: luu trang thai hoac du lieu tam cho updated at.
    private long updatedAt;
    // Ham tao: khoi tao doi tuong User voi cac phu thuoc can thiet.
    public User() {
        super();
    }
    // Ham tao: khoi tao doi tuong User voi cac phu thuoc can thiet.
    public User(int id, String username, String password, String fullName, String email, Role role, String status, long createdAt, long updatedAt) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get username.
    public String getUsername() { return username; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set username.
    public void setUsername(String username) { this.username = username; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get password.
    public String getPassword() { return password; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set password.
    public void setPassword(String password) { this.password = password; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get full name.
    public String getFullName() { return fullName; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set full name.
    public void setFullName(String fullName) { this.fullName = fullName; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get email.
    public String getEmail() { return email; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set email.
    public void setEmail(String email) { this.email = email; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get role.
    public Role getRole() { return role; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set role.
    public void setRole(Role role) { this.role = role; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get status.
    public String getStatus() { return status; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set status.
    public void setStatus(String status) { this.status = status; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get created at.
    public long getCreatedAt() { return createdAt; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set created at.
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get updated at.
    public long getUpdatedAt() { return updatedAt; }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set updated at.
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get role name.
    public String getRoleName() {
        return role == null ? "UNKNOWN" : role.name();
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac check password.
    public boolean checkPassword(String inputPassword) {
        return PasswordUtil.verifyPassword(inputPassword, this.password);
    }

    @Override
    // Phuong thuc: thuc hien chuc nang to string trong lop User.
    public String toString() {
        return id + "," + username + "," + password + "," + fullName + "," + email + "," + role + "," + status + "," + createdAt + "," + updatedAt;
    }
}
