package userauth.exception;

// File note: Ngoại lệ dùng cho các lỗi validate nghiệp vụ và dữ liệu đầu vào.
public class ValidationException extends Exception {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}

