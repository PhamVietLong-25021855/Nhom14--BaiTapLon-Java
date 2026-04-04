import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // 1. Thêm công cụ định dạng thời gian
import java.nio.charset.StandardCharsets;  // 2. Thêm công cụ chuẩn hóa Tiếng Việt
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // 3. Ép Scanner đọc đúng Tiếng Việt có dấu
        System.setOut(new java.io.PrintStream(System.out, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        System.out.println("=== HỆ THỐNG QUẢN LÝ ĐẤU GIÁ (DÀNH CHO SELLER) ===\n");

        try {
            System.out.println("Vui lòng chọn loại sản phẩm muốn đăng:");
            System.out.println("1. Đồ Điện tử");
            System.out.println("2. Đồ Thể thao");
            System.out.println("3. Tác phẩm Nghệ thuật");
            System.out.print("Lựa chọn của bạn (1/2/3): ");
            int choice = Integer.parseInt(scanner.nextLine());

            System.out.println("\n--- NHẬP THÔNG TIN CHUNG ---");
            System.out.print("Nhập tên sản phẩm: ");
            String name = scanner.nextLine();

            System.out.print("Nhập mô tả sản phẩm: ");
            String description = scanner.nextLine();

            System.out.print("Nhập giá khởi điểm (VNĐ): ");
            double startPrice = Double.parseDouble(scanner.nextLine());

            System.out.print("Nhập số ngày muốn mở đấu giá (ví dụ: 3): ");
            int durationDays = Integer.parseInt(scanner.nextLine());
            
            LocalDateTime startTime = LocalDateTime.now();
            LocalDateTime endTime = startTime.plusDays(durationDays);
            String sellerId = "SELLER_CURRENT_USER";

            Map<String, Object> extraParams = new HashMap<>();
            ItemType type = null;

            System.out.println("\n--- NHẬP THÔNG TIN CHI TIẾT ---");
            switch (choice) {
                case 1:
                    type = ItemType.ELECTRONIC;
                    extraParams.put("typeProduct", "Đồ điện tử");
                    System.out.print("Nhập số tháng bảo hành: ");
                    int warranty = Integer.parseInt(scanner.nextLine());
                    extraParams.put("warrantyMonths", warranty);
                    break;
                case 2:
                    type = ItemType.SPORT;
                    extraParams.put("typeProduct", "Đồ thể thao");
                    System.out.print("Nhập thương hiệu (VD: Mizuno, Nike): ");
                    String brand = scanner.nextLine();
                    extraParams.put("brand", brand);
                    break;
                case 3:
                    type = ItemType.ART;
                    extraParams.put("typeProduct", "Tác phẩm nghệ thuật");
                    System.out.print("Nhập tên tác giả: ");
                    String author = scanner.nextLine();
                    extraParams.put("author", author);
                    break;
                default:
                    System.out.println("Lựa chọn không hợp lệ. Đang hủy thao tác...");
                    scanner.close();
                    return;
            }

            System.out.println("\nĐang xử lý dữ liệu...");
            Item newItem = ItemFactory.createItem(
                name, description, startPrice, startTime, endTime, 
                sellerId, extraParams, type
            );

            // 4. Tạo khuôn định dạng ngày giờ chuẩn Việt Nam
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            System.out.println("\n✅ ĐĂNG SẢN PHẨM THÀNH CÔNG!");
            System.out.println("Thông tin chi tiết: " + newItem.getDetails());
            
            // 5. In thời gian qua cái khuôn đã tạo
            System.out.println("Thời gian mở cửa: " + newItem.getStartTime().format(formatter));
            System.out.println("Thời gian đóng cửa: " + newItem.getEndTime().format(formatter));

        } catch (NumberFormatException e) {
            System.err.println("❌ Lỗi: Bạn phải nhập đúng định dạng số (không chứa chữ cái)!");
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Lỗi dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Lỗi hệ thống: " + e.getMessage());
        } finally {
            scanner.close(); 
        }
    }
}