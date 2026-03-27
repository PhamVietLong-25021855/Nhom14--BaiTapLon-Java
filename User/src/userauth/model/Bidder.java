package userauth.model;

public class Bidder extends User {
    public Bidder() {
        super();
        setRole(Role.BIDDER);
    }

    public Bidder(int id, String username, String password, String email) {
        super(id, username, password, email, Role.BIDDER);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }
}
