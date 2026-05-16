package userauth.model;

import userauth.util.PasswordUtil;

/**
 * Lớp cha trừu tượng cho tất cả loại tài khoản trong hệ thống.
 *
 * Các lớp Admin, Seller, Bidder đều kế thừa từ User.
 * User chứa những thông tin chung nhất của tài khoản như username, password,
 * họ tên, email, vai trò, trạng thái và thời gian tạo/cập nhật.
 */
public abstract class User extends Entity {
    /** Tên đăng nhập, thường là duy nhất trong hệ thống. */
    private String username;

    /** Mật khẩu đã được mã hóa/hash, không nên lưu mật khẩu thô. */
    private String password;

    /** Họ tên đầy đủ của người dùng. */
    private String fullName;

    /** Email dùng để liên hệ hoặc đăng ký tài khoản. */
    private String email;

    /** Vai trò của tài khoản: BIDDER, SELLER hoặc ADMIN. */
    private Role role;

    /** Trạng thái tài khoản, ví dụ ACTIVE hoặc BLOCKED. */
    private String status;

    /** Thời điểm tạo tài khoản, lưu dạng millisecond. */
    private long createdAt;

    /** Thời điểm cập nhật gần nhất, lưu dạng millisecond. */
    private long updatedAt;

    /**
     * Constructor rỗng cho trường hợp cần tạo User trước rồi gán dữ liệu sau.
     */
    public User() {
        super();
    }

    /**
     * Constructor đầy đủ dùng khi tạo hoặc đọc một tài khoản từ database.
     *
     * @param id id tài khoản
     * @param username tên đăng nhập
     * @param password mật khẩu đã hash
     * @param fullName họ tên người dùng
     * @param email email người dùng
     * @param role vai trò tài khoản
     * @param status trạng thái tài khoản
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
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

    /** @return tên đăng nhập của người dùng */
    public String getUsername() { return username; }

    /** @param username tên đăng nhập mới cần gán */
    public void setUsername(String username) { this.username = username; }

    /** @return mật khẩu đã mã hóa/hash */
    public String getPassword() { return password; }

    /** @param password mật khẩu đã mã hóa/hash cần gán */
    public void setPassword(String password) { this.password = password; }

    /** @return họ tên đầy đủ của người dùng */
    public String getFullName() { return fullName; }

    /** @param fullName họ tên mới cần gán */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** @return email của người dùng */
    public String getEmail() { return email; }

    /** @param email email mới cần gán */
    public void setEmail(String email) { this.email = email; }

    /** @return vai trò hiện tại của tài khoản */
    public Role getRole() { return role; }

    /** @param role vai trò mới cần gán */
    public void setRole(Role role) { this.role = role; }

    /** @return trạng thái tài khoản, ví dụ ACTIVE hoặc BLOCKED */
    public String getStatus() { return status; }

    /** @param status trạng thái mới của tài khoản */
    public void setStatus(String status) { this.status = status; }

    /** @return thời điểm tạo tài khoản */
    public long getCreatedAt() { return createdAt; }

    /** @param createdAt thời điểm tạo cần gán */
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    /** @return thời điểm cập nhật gần nhất */
    public long getUpdatedAt() { return updatedAt; }

    /** @param updatedAt thời điểm cập nhật mới */
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Lấy tên vai trò dưới dạng chuỗi.
     *
     * Hàm này tránh lỗi NullPointerException nếu role chưa được gán.
     * Nó thường được dùng khi hiển thị vai trò trên giao diện hoặc khi ghi log.
     *
     * @return tên role, hoặc UNKNOWN nếu role null
     */
    public String getRoleName() {
        return role == null ? "UNKNOWN" : role.name();
    }

    /**
     * Kiểm tra mật khẩu người dùng nhập vào có khớp với mật khẩu đã lưu hay không.
     *
     * Luồng xử lý:
     * 1. Người dùng nhập mật khẩu ở màn hình login.
     * 2. AuthService/UserDAO lấy User từ database.
     * 3. Hàm này gọi PasswordUtil.verifyPassword để so sánh mật khẩu nhập với mật khẩu hash.
     *
     * @param inputPassword mật khẩu người dùng vừa nhập
     * @return true nếu đúng mật khẩu, false nếu sai
     */
    public boolean checkPassword(String inputPassword) {
        return PasswordUtil.verifyPassword(inputPassword, this.password);
    }

    /**
     * Chuyển thông tin user thành chuỗi ngăn cách bởi dấu phẩy.
     *
     * Hàm này chủ yếu phục vụ debug, ghi log hoặc xuất dữ liệu đơn giản.
     * Không nên dùng để hiển thị mật khẩu trong giao diện thật vì có chứa trường password.
     *
     * @return chuỗi chứa các thông tin chính của User
     */
    @Override
    public String toString() {
        return id + "," + username + "," + password + "," + fullName + "," + email + "," + role + "," + status + "," + createdAt + "," + updatedAt;
    }
}
