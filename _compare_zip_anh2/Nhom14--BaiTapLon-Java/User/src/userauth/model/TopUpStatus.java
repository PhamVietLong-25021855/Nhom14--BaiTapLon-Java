package userauth.model;

public enum TopUpStatus {
    PENDING,    // Đang chờ thanh toán
    SUCCESS,    // Đã thanh toán thành công
    FAILED,     // Thanh toán thất bại
    CANCELLED   // Đã hủy
}
