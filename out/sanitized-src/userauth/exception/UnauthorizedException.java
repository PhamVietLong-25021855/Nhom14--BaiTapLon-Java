package userauth.exception;

// File note: Ngoại lệ báo người dùng hiện tại không có quyền thực hiện thao tác.
public class UnauthorizedException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnauthorizedException(String message) {
        super(message);
    }
}

