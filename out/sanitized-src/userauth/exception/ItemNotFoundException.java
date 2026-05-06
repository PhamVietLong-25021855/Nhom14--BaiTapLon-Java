package userauth.exception;

// File note: Ngoại lệ báo không tìm thấy dữ liệu cần thao tác.
public class ItemNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public ItemNotFoundException(String message) {
        super(message);
    }
}

