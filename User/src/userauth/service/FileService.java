package userauth.service;

import userauth.model.Admin;
import userauth.model.Bidder;
import userauth.model.Role;
import userauth.model.Seller;
import userauth.model.User;

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
                    if (parts.length != 5) {
                        System.out.println("Dòng không hợp lệ, bỏ qua: " + line);
                        continue;
                    }

                    int id = Integer.parseInt(parts[0].trim());
                    String username = parts[1].trim();
                    String password = parts[2].trim(); // password đã hash
                    String email = parts[3].trim();
                    Role role = Role.valueOf(parts[4].trim().toUpperCase());

                    User user = createUserByRole(id, username, password, email, role);
                    if (user != null) {
                        users.add(user);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            System.out.println("Lỗi đọc file users.txt: " + e.getMessage());
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
                    bw.write(
                            user.getId() + "," +
                                    user.getUsername() + "," +
                                    user.getPassword() + "," +
                                    user.getEmail() + "," +
                                    user.getRole()
                    );
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi ghi file users.txt: " + e.getMessage());
        }
    }

    private User createUserByRole(int id, String username, String password, String email, Role role) {
        switch (role) {
            case ADMIN:
                return new Admin(id, username, password, email);
            case SELLER:
                return new Seller(id, username, password, email);
            case BIDDER:
                return new Bidder(id, username, password, email);
            default:
                return null;
        }
    }
}
