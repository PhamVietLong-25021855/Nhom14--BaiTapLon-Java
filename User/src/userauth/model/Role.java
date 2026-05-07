package userauth.model;

/**
 * Enum mô tả vai trò của tài khoản trong hệ thống đấu giá.
 *
 * Vai trò được dùng để:
 * - Phân quyền đăng nhập.
 * - Điều hướng màn hình sau khi đăng nhập.
 * - Kiểm tra người dùng có được phép thực hiện hành động hay không.
 */
public enum Role {
    /** Người tham gia đấu giá, có thể đặt giá và thiết lập đấu giá tự động. */
    BIDDER,

    /** Người bán, có thể tạo và quản lý phiên đấu giá của mình. */
    SELLER,

    /** Quản trị viên, có quyền quản lý hệ thống và can thiệp phiên đấu giá khi cần. */
    ADMIN
}
