package userauth.model;

public class Seller extends User {

    public Seller(int id, String username, String password, String fullName, String email, String status, long createdAt, long updatedAt) {
        super(id, username, password, fullName, email, Role.SELLER, status, createdAt, updatedAt);
    }
}
