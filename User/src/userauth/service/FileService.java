package userauth.service;

import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;
import userauth.utils.ConsoleUI;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String FILE_PATH = "User/data/users.txt";

    public List<User> loadUsersFromFile() {
        List<User> users = new ArrayList<>();
        File file = new File(FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    User user = parseUser(line);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi doc file users.txt: " + ex.getMessage());
        }

        return users;
    }

    public void saveUsersToFile(List<User> users) {
        File file = new File(FILE_PATH);

        try {
            ensureFileExists(file);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (User user : users) {
                    writer.write(user.toString());
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            ConsoleUI.printError("Loi ghi file users.txt: " + ex.getMessage());
        }
    }

    private User parseUser(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 9) {
            ConsoleUI.printWarning("Dong user khong hop le, bo qua: " + line);
            return null;
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
            return createUserByRole(id, username, password, fullName, email, role, status, createdAt, updatedAt);
        } catch (Exception ex) {
            ConsoleUI.printError("Khong the parse dong user: " + line);
            return null;
        }
    }

    private User createUserByRole(int id, String username, String password, String fullName, String email, Role role, String status, long createdAt, long updatedAt) {
        return switch (role) {
            case ADMIN -> new Admin(id, username, password, fullName, email, status, createdAt, updatedAt);
            case SELLER -> new Seller(id, username, password, fullName, email, status, createdAt, updatedAt);
            case BIDDER -> new Bidder(id, username, password, fullName, email, status, createdAt, updatedAt);
        };
    }

    private void ensureFileExists(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
    }
}
