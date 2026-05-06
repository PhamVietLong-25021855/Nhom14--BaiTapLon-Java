package userauth.validation;

import java.util.regex.Pattern;
// File note: Helper validate dữ liệu người dùng trước khi tạo/cập nhật tài khoản.
public class UserValidator {

    private UserValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null
                && !username.isEmpty()
                && username.length() >= 6
                && username.length() <= 20;
    }

    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 6
                && password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$");
    }

    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public static boolean isValidEmail(String email) {
        // 1. null hoáº·c rá»—ng
        if (email == null || email.isBlank()) {
            return false;
        }

        // 2. loáº¡i bá» khoáº£ng tráº¯ng dÆ°
        email = email.trim();

        // 3. kiá»ƒm tra báº±ng regex
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
