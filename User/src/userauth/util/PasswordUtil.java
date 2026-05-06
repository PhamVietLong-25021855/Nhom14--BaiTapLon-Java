package userauth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// Ghi chu file: File tien ich dung chung; gom cac ham phu tro cho xu ly toan ung dung.
// Khai bao lop PasswordUtil; cung cap ham tien ich dung lai trong nhieu noi.
public class PasswordUtil {
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac hash password.
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    // Phuong thuc: kiem tra dieu kien hoac xac thuc cho thao tac verify password.
    public static boolean verifyPassword(String inputPassword, String storedHash) {
        return hashPassword(inputPassword).equals(storedHash);
    }
}
