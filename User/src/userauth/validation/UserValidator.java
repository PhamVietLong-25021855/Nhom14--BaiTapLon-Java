package userauth.validation;

import java.util.regex.Pattern;

// Ghi chu file: File validation; kiem tra du lieu dau vao truoc khi dua vao nghiep vu hoac database.
// Khai bao lop UserValidator; tap trung logic kiem tra hop le du lieu dau vao.
public class UserValidator {
    // Ham tao: khoi tao doi tuong UserValidator voi cac phu thuoc can thiet.
    private UserValidator() {
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is valid username.
    public static boolean isValidUsername(String username) {
        return username != null
                && !username.isEmpty()
                && username.length() >= 6
                && username.length() <= 20;
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is valid password.
    public static boolean isValidPassword(String password) {
        return password != null
                && password.length() >= 6
                && password.matches("^(?=.*[A-Za-z])(?=.*\\d).+$");
    }
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho regex.
    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac is valid email.
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
