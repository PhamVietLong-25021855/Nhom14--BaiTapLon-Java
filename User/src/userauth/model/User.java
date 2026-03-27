package userauth.model;

import userauth.util.PasswordUtil;

public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;
    private Role role;

    public User() {
        super();
    }

    public User(int id, String username, String password, String email, Role role) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // password ở đây là password đã hash
    public String getPassword() {
        return password;
    }

    // truyền vào hashed password
    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getRoleName() {
        return role == null ? "UNKNOWN" : role.name();
    }

    public boolean checkPassword(String inputPassword) {
        return PasswordUtil.verifyPassword(inputPassword, this.password);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{"
                + "id=" + id
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", role=" + role
                + '}';
    }
}