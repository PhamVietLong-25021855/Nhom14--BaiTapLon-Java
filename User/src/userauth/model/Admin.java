package userauth.model;

public class Admin extends User {
    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int id, String username, String password, String email) {
        super(id, username, password, email, Role.ADMIN);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }
}
