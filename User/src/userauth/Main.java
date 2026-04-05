package userauth;

import userauth.controller.AuthController;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;
import userauth.validation.UserValidator;

import java.util.Scanner;

public class Main {

      static void main(String[] args) {
        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        Scanner scanner = new Scanner(System.in);
        int choice = 0;

        do {
            printMenu();
            choice = readInt(scanner, "Chọn chức năng: ");

            switch (choice) {
                case 1:
                    handleRegister(scanner, authController);
                    break;

                case 2:
                    handleLogin(scanner, authController);
                    break;

                case 3:
                    authController.showAllUsers();
                    break;

                case 4:
                    System.out.println("Thoát chương trình.");
                    break;

                default:
                    System.out.println("Lựa chọn không hợp lệ.");
            }

        } while (choice != 4);

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n===== MENU USER/AUTH =====");
        System.out.println("1. Đăng ký");
        System.out.println("2. Đăng nhập");
        System.out.println("3. Xem danh sách user");
        System.out.println("4. Thoát");
    }

    private static void handleRegister(Scanner scanner, AuthController authController) {
        System.out.print("Nhập username: ");
        String regUsername = scanner.nextLine().trim();

        String email;
        while (true) {
            System.out.print("Nhập email: ");
            email = scanner.nextLine();

            if (UserValidator.isValidEmail(email)) {
                break;
            }

            System.out.println("Email không hợp lệ. Vui lòng nhập lại.");
        }

        String password;
        while (true) {
            System.out.print("Nhập mật khẩu: ");
            password = scanner.nextLine();

            if (UserValidator.isValidPassword(password)) {
                break;
            }

            System.out.println("Mật khẩu không hợp lệ. Phải có ít nhất 6 ký tự, gồm chữ và số.");
        }

        Role role = readRole(scanner);

        if (role == null) {
            System.out.println("Role không hợp lệ. Hủy đăng ký.");
            return;
        }
        
        authController.register(regUsername, password, email, role);
    }

    private static void handleLogin(Scanner scanner, AuthController authController) {
        while (true) {
            System.out.print("Nhập username: ");
            String loginUsername = scanner.nextLine().trim();

            System.out.print("Nhập password: ");
            String loginPassword = scanner.nextLine();

            User loggedInUser = authController.login(loginUsername, loginPassword);

            if (loggedInUser != null) {
                System.out.println("Đăng nhập thành công!");
                authController.checkRole(loggedInUser);
                break;
            } else {
                System.out.println("Sai username hoặc mật khẩu. Vui lòng nhập lại!\n");
            }
        }
    }

    private static Role readRole(Scanner scanner) {
        System.out.println("Chọn role:");
        System.out.println("1. BIDDER");
        System.out.println("2. SELLER");
        System.out.println("3. ADMIN");

        int roleChoice = readInt(scanner, "Nhập lựa chọn: ");

        switch (roleChoice) {
            case 1:
                return Role.BIDDER;
            case 2:
                return Role.SELLER;
            case 3:
                return Role.ADMIN;
            default:
                return null;
        }
    }

    private static int readInt(Scanner scanner, String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();

            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập số hợp lệ.");
            }
        }
    }
}