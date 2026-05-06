package userauth.model;

// File note: Lớp cha tối giản cho entity có khóa id.
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

    public void setId(int id) {
        this.id = id;
    }
}

