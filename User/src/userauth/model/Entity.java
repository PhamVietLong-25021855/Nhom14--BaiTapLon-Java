package userauth.model;

// Ghi chu file: File model; luu cau truc du lieu va thuoc tinh cua doi tuong nghiep vu.
// Khai bao lop Entity; mo ta cau truc du lieu cua doi tuong nghiep vu.
public abstract class Entity {
    // Thuoc tinh: luu trang thai hoac du lieu tam cho id.
    protected int id;
    // Ham tao: khoi tao doi tuong Entity voi cac phu thuoc can thiet.
    public Entity(int id) {
        this.id = id;
    }
    // Ham tao: khoi tao doi tuong Entity voi cac phu thuoc can thiet.
    public Entity() {
    }
    // Phuong thuc: lay hoac doc du lieu cho thao tac get id.
    public int getId() {
        return id;
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set id.
    public void setId(int id) {
        this.id = id;
    }
}
