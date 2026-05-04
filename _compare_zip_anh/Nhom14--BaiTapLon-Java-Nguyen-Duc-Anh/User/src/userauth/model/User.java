package userauth.model;

import userauth.util.PasswordUtil;

public abstract class User extends Entity {
    private String username;
    private String password;
    private String fullName;
    private String email;
    private Role role;
    private String status; // ACTIVE, BLOCKED
    private long createdAt;
    private long updatedAt;

    public User() {
        super();
    }

    public User(int id, String username, String password, String fullName, String email, Role role, String status, long createdAt, long updatedAt) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getRoleName() {
        return role == null ? "UNKNOWN" : role.name();
    }

    public boolean checkPassword(String inputPassword) {
        return PasswordUtil.verifyPassword(inputPassword, this.password);
    }

    @Override
    public String toString() {
        return id + "," + username + "," + password + "," + fullName + "," + email + "," + role + "," + status + "," + createdAt + "," + updatedAt;
    }
}