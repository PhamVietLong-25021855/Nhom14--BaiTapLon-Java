package userauth.model;

/**
 * Model đại diện cho tài khoản quản trị viên.
 *
 * Admin kế thừa toàn bộ thông tin chung từ User và luôn có Role.ADMIN.
 * Ngoài ra Admin có thêm department để mô tả bộ phận/phạm vi quản trị.
 */
public class Admin extends User {
    /** Bộ phận quản lý của admin, mặc định có thể là SYSTEM. */
    private String department;

    /**
     * Constructor tạo Admin với department mặc định là SYSTEM.
     *
     * @param id id tài khoản admin
     * @param username tên đăng nhập
     * @param password mật khẩu đã hash
     * @param fullName họ tên admin
     * @param email email admin
     * @param status trạng thái tài khoản
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = "SYSTEM";
    }

    /**
     * Constructor tạo Admin với department truyền từ bên ngoài.
     *
     * @param id id tài khoản admin
     * @param username tên đăng nhập
     * @param password mật khẩu đã hash
     * @param fullName họ tên admin
     * @param email email admin
     * @param status trạng thái tài khoản
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     * @param department bộ phận quản lý của admin
     */
    public Admin(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt, String department) {
        super(id, username, password, fullName, email, Role.ADMIN, status, createdAt, updatedAt);
        this.department = department;
    }

    /** @return tên bộ phận/phạm vi quản trị của admin */
    public String getDepartment() {
        return department;
    }

    /** @param department bộ phận/phạm vi quản trị mới */
    public void setDepartment(String department) {
        this.department = department;
    }
}
