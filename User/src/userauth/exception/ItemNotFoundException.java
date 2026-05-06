package userauth.exception;

// Ghi chu file: File ngoai le nghiep vu; tach rieng tung loai loi de xu ly ro nghia hon.
// Khai bao lop ItemNotFoundException; dai dien mot tinh huong loi nghiep vu cu the.
public class ItemNotFoundException extends Exception {
    // Thuoc tinh/hang so: luu cau hinh hoac gia tri dung chung cho serial version uid.
    private static final long serialVersionUID = 1L;
    // Ham tao: khoi tao doi tuong ItemNotFoundException voi cac phu thuoc can thiet.
    public ItemNotFoundException(String message) {
        super(message);
    }
}
