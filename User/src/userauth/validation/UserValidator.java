package userauth.validation;

import userauth.model.Role;

public class UserValidator {

    public static void validateRegister(String username, String email, String password, Role role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống");
        }

        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Username phải có ít nhất 3 ký tự");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email không được để trống");
        }

        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password không được để trống");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Password phải có ít nhất 6 ký tự");
        }

        if (role == null) {
            throw new IllegalArgumentException("Vai trò không hợp lệ");
        }
    }

    public static void validateLogin(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username không được để trống");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password không được để trống");
        }
    }

    private static boolean isValidEmail(String email) {
        return email.contains("@") && email.contains(".");
    }
}