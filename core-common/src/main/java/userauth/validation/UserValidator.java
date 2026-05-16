package userauth.validation;

import java.util.regex.Pattern;

public class UserValidator {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private UserValidator() {
    }

    public static boolean isValidUsername(String username) {
        return username != null && !username.isEmpty() && username.length() >= 6 && username.length() <= 20;
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6 && password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$");
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        email = email.trim();
        return EMAIL_PATTERN.matcher(email).matches();
    }
}
