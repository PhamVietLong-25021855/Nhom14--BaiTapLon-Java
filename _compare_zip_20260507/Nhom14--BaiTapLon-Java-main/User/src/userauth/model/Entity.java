package userauth.model;

/**
 * Lớp cha chung cho các đối tượng chính trong hệ thống.
 *
 * Ý nghĩa:
 * - Hầu hết các bảng trong database đều có khóa chính id.
 * - Những model như User, Item, AutoBid, BidTransaction kế thừa Entity để dùng chung thuộc tính id.
 * - Nhờ đó code ở tầng DAO/Service có thể xử lý các đối tượng có id theo một cách thống nhất.
 */
public abstract class Entity {
    /**
     * Mã định danh duy nhất của đối tượng.
     * Trường này thường tương ứng với cột id trong database.
     */
    protected int id;

    /**
     * Constructor dùng khi đã có id, ví dụ khi đọc dữ liệu từ database.
     *
     * @param id mã định danh của đối tượng
     */
    public Entity(int id) {
        this.id = id;
    }

    /**
     * Constructor rỗng dùng khi cần tạo object trước rồi gán id sau.
     * Ví dụ: một số framework hoặc thao tác mapping dữ liệu có thể cần constructor mặc định.
     */
    public Entity() {
    }

    /**
     * Lấy id của đối tượng.
     *
     * @return id hiện tại
     */
    public int getId() {
        return id;
    }

    /**
     * Gán/cập nhật id cho đối tượng.
     *
     * @param id id mới cần gán
     */
    public void setId(int id) {
        this.id = id;
    }
}
