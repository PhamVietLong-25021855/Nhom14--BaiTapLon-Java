package userauth.model;

public class Seller extends User {
    public Seller() {
        super();
        setRole(Role.SELLER);
    }

    public Seller(int id, String username, String password, String email) {
        super(id, username, password, email, Role.SELLER);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }
}
