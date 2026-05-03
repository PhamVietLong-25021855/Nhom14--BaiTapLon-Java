package userauth.validation;

import java.util.regex.Pattern;
public class UserValidator {

    private UserValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null
                && !username.isBlank()
                && username.length() >= 3
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
        // 1. null hoặc rỗng
        if (email == null || email.isBlank()) {
            return false;
        }

        // 2. loại bỏ khoảng trắng dư
        email = email.trim();

        // 3. kiểm tra bằng regex
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
