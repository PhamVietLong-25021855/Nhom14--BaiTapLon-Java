package userauth.controller;

import userauth.model.Role;
import userauth.model.User;
import userauth.service.AuthService;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public void register(String username, String password, String email, Role role) {
        String result = authService.register(username, password, email, role);
        System.out.println(result);
    }

    public User login(String username, String password) {
        return authService.login(username, password);
    }

    public void checkRole(User user) {
        if (user == null) {
            return;
        }

        if (authService.isAdmin(user)) {
            System.out.println("Bạn là ADMIN");
        } else if (authService.isSeller(user)) {
            System.out.println("Bạn là SELLER");
        } else if (authService.isBidder(user)) {
            System.out.println("Bạn là BIDDER");
        }
    }

    public void showAllUsers() {
        for (User user : authService.getAllUsers()) {
            System.out.println(user);
        }
    }
}