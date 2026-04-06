package userauth.model;

public abstract class Entity {
    protected int id;

    public Entity(int id) {
        this.id = id;
    }

    public Entity() {
    }

    public int getId() {
        return id;
    }

    // Phương thức trừu tượng - các lớp con bắt buộc override (Polymorphism)
    public abstract void printInfo();
}