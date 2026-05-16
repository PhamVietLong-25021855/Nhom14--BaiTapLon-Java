package userauth.model;

/**
 * Model đại diện cho người bán trong hệ thống.
 *
 * Seller kế thừa User và luôn có Role.SELLER.
 * Người bán thường được phép tạo phiên đấu giá, chỉnh sửa sản phẩm của mình,
 * theo dõi phiên đấu giá và xem kết quả bán hàng.
 */
public class Seller extends User {

    /**
     * Constructor tạo một tài khoản người bán.
     *
     * @param id id tài khoản seller
     * @param username tên đăng nhập
     * @param password mật khẩu đã hash
     * @param fullName họ tên người bán
     * @param email email người bán
     * @param status trạng thái tài khoản
     * @param createdAt thời điểm tạo
     * @param updatedAt thời điểm cập nhật
     */
    public Seller(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.SELLER, status, createdAt, updatedAt);
    }
}
