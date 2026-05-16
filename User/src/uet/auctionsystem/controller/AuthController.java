package uet.auctionsystem.controller;

import uet.auctionsystem.model.Role;
import uet.auctionsystem.model.User;
import uet.auctionsystem.service.AuthService;
import uet.auctionsystem.exception.UnauthorizedException;
import uet.auctionsystem.exception.ValidationException;
import java.util.List;

// Ghi chu file: File controller nam giua giao dien va service; nhan lenh tu UI va goi nghiep vu tuong ung.
// Khai bao lop AuthController; dieu phoi thao tac UI va chuyen tiep yeu cau xu ly nghiep vu.
public class AuthController {
    // Thuoc tinh: giu tham chieu den AuthService de phoi hop xu ly.
    private final AuthService authService;
    // Ham tao: khoi tao doi tuong AuthController voi cac phu thuoc can thiet.
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac register gui.
    public String registerGUI(String username, String password, String fullName, String email, Role role) {
        try {
            authService.register(username, password, fullName, email, role);
            return "SUCCESS";
        } catch (ValidationException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: thuc hien chuc nang login trong lop AuthController.
    public User login(String username, String password) throws UnauthorizedException {
        return authService.login(username, password); // Will throw UnauthorizedException if fails
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get all users list.
    public List<User> getAllUsersList() {
        return authService.getAllUsers();
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac change password.
    public String changePassword(String username, String oldPassword, String newPassword) {
        try {
            authService.changePassword(username, oldPassword, newPassword);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: thuc hien chuc nang promote user to admin trong lop AuthController.
    public String promoteUserToAdmin(String adminUsername, int targetUserId) {
        try {
            authService.promoteUserToAdmin(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: thuc hien chuc nang demote admin to bidder trong lop AuthController.
    public String demoteAdminToBidder(String adminUsername, int targetUserId) {
        try {
            authService.demoteAdminToBidder(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
    // Phuong thuc: thuc hien chuc nang toggle user status trong lop AuthController.
    public String toggleUserStatus(String adminUsername, int targetUserId) {
        try {
            authService.toggleUserStatus(adminUsername, targetUserId);
            return "SUCCESS";
        } catch (ValidationException | UnauthorizedException e) {
            return e.getMessage();
        }
    }
}
