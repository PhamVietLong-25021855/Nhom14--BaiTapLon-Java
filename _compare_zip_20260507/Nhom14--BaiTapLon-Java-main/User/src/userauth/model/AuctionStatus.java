package userauth.model;

/**
 * Enum biểu diễn trạng thái của một phiên đấu giá.
 *
 * Trạng thái này giúp service/controller biết phiên đấu giá đang ở giai đoạn nào
 * để quyết định có cho đặt giá, chỉnh sửa, hủy, thanh toán hay đóng phiên hay không.
 */
public enum AuctionStatus {
    /** Phiên đã được tạo nhưng chưa đến thời gian bắt đầu. */
    OPEN,

    /** Phiên đang diễn ra và có thể nhận giá đặt. */
    RUNNING,

    /** Phiên đã kết thúc do hết thời gian hoặc bị đóng sớm. */
    FINISHED,

    /** Người thắng đã thanh toán xong. */
    PAID,

    /** Phiên bị hủy, không còn được tham gia đấu giá. */
    CANCELED;
}
