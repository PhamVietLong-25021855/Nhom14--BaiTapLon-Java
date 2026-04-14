package userauth.service;

import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;
import userauth.utils.ConsoleUI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String FILE_PATH = "User/data/users.txt";

    public List<User> loadUsersFromFile() {
        List<User> users = new ArrayList<>();
        File file = new File(FILE_PATH);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            if (!file.exists()) {
                file.createNewFile();
                return users;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    String[] parts = line.split(",", -1);
                    if (parts.length < 9) {
                        ConsoleUI.printWarning("Dòng không hợp lệ, bỏ qua: " + line);
                        continue;
                    }

                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String username = parts[1].trim();
                        String password = parts[2].trim(); 
                        String fullName = parts[3].trim();
                        String email = parts[4].trim();
                        Role role = Role.valueOf(parts[5].trim().toUpperCase());
                        String status = parts[6].trim();
                        long createdAt = Long.parseLong(parts[7].trim());
                        long updatedAt = Long.parseLong(parts[8].trim());

                        User user = createUserByRole(id, username, password, fullName, email, role, status, createdAt, updatedAt);
                        if (user != null) {
                            users.add(user);
                        }
                    } catch (Exception ex) {
                         ConsoleUI.printError("Lỗi parse dòng user: " + line);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            ConsoleUI.printError("Lỗi đọc file users.txt: " + e.getMessage());
        }

        return users;
    }

    public void saveUsersToFile(List<User> users) {
        File file = new File(FILE_PATH);

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (User user : users) {
                    bw.write(user.toString());
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            ConsoleUI.printError("Lỗi ghi file users.txt: " + e.getMessage());
        }
    }

    private User createUserByRole(int id, String username, String password, String fullName, String email, Role role, String status, long createdAt, long updatedAt) {
        switch (role) {
            case ADMIN:
                return new Admin(id, username, password, fullName, email, status, createdAt, updatedAt);
            case SELLER:
                return new Seller(id, username, password, fullName, email, status, createdAt, updatedAt);
            case BIDDER:
                return new Bidder(id, username, password, fullName, email, status, createdAt, updatedAt);
            default:
                return null;
        }
    }
}
