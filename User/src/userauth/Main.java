package userauth;

import userauth.controller.AuthController;
import userauth.dao.UserDAO;
import userauth.dao.UserDAOImpl;
import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDAOImpl();
        AuthService authService = new AuthService(userDAO);
        AuthController authController = new AuthController(authService);

        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\n===== MENU USER/AUTH =====");
            System.out.println("1. Đăng ký");
            System.out.println("2. Đăng nhập");
            System.out.println("3. Xem danh sách user");
            System.out.println("4. Thoát");
            System.out.print("Chọn chức năng: ");

            choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1:
                    System.out.print("Nhập username: ");
                    String regUsername = scanner.nextLine();

                    System.out.print("Nhập email: ");
                    String regEmail = scanner.nextLine();

                    System.out.print("Nhập password: ");
                    String regPassword = scanner.nextLine();

                    System.out.println("Chọn role:");
                    System.out.println("1. BIDDER");
                    System.out.println("2. SELLER");
                    System.out.println("3. ADMIN");
                    System.out.print("Nhập lựa chọn: ");
                    int roleChoice = Integer.parseInt(scanner.nextLine());

                    Role role = null;
                    switch (roleChoice) {
                        case 1:
                            role = Role.BIDDER;
                            break;
                        case 2:
                            role = Role.SELLER;
                            break;
                        case 3:
                            role = Role.ADMIN;
                            break;
                        default:
                            System.out.println("Role không hợp lệ");
                    }

                    if (role != null) {
                        authController.register(regUsername, regEmail, regPassword, role);
                    }
                    break;

                case 2:
                    System.out.print("Nhập username: ");
                    String loginUsername = scanner.nextLine();

                    System.out.print("Nhập password: ");
                    String loginPassword = scanner.nextLine();

                    User loggedInUser = authController.login(loginUsername, loginPassword);
                    authController.checkRole(loggedInUser);
                    break;

                case 3:
                    authController.showAllUsers();
                    break;

                case 4:
                    System.out.println("Thoát chương trình");
                    break;

                default:
                    System.out.println("Lựa chọn không hợp lệ");
            }
        } while (choice != 4);

        scanner.close();
    }
}