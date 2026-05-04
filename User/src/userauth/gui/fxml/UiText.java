package userauth.gui.fxml;

import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ComboBoxBase;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.util.StringConverter;
import userauth.model.AuctionStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class UiText {
    private static final Map<String, String> EN_TO_VI = createTranslations();
    private static final Map<String, String> VI_TO_EN = createReverseTranslations(EN_TO_VI);
    private static final Map<String, String> EN_PREFIX_TO_VI = createPrefixTranslations();
    private static final Map<String, String> VI_PREFIX_TO_EN = createReverseTranslations(EN_PREFIX_TO_VI);
    private static final List<Map.Entry<String, String>> EN_PREFIXES = sortByKeyLengthDesc(EN_PREFIX_TO_VI);
    private static final List<Map.Entry<String, String>> VI_PREFIXES = sortByKeyLengthDesc(VI_PREFIX_TO_EN);

    private static AppLanguage currentLanguage = AppLanguage.ENGLISH;

    private UiText() {
    }

    static void setCurrentLanguage(AppLanguage language) {
        currentLanguage = language == null ? AppLanguage.ENGLISH : language;
    }

    static String text(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Map<String, String> exact = currentLanguage == AppLanguage.VIETNAMESE ? EN_TO_VI : VI_TO_EN;
        List<Map.Entry<String, String>> prefixes = currentLanguage == AppLanguage.VIETNAMESE ? EN_PREFIXES : VI_PREFIXES;

        String exactMatch = exact.get(value);
        if (exactMatch != null) {
            return exactMatch;
        }

        return translateStructured(value, exact, prefixes);
    }

    static String auctionStatus(AuctionStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case RUNNING -> text("RUNNING");
            case OPEN -> text("OPENING SOON");
            case FINISHED -> text("FINISHED");
            case CANCELED -> text("CANCELLED");
            case PAID -> text("PAID");
        };
    }

    static String userStatus(String status) {
        return text(status);
    }

    static void apply(Node root) {
        if (root == null) {
            return;
        }
        applyNode(root);
    }

    static void configureTranslatedComboBox(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : text(value);
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });

        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text(item));
            }
        });

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text(item));
            }
        });

        refreshTranslatedComboBox(comboBox);
    }

    static void refreshTranslatedComboBox(ComboBox<String> comboBox) {
        if (comboBox == null) {
            return;
        }

        if (comboBox.getButtonCell() != null) {
            String value = comboBox.getValue();
            comboBox.getButtonCell().setText(value == null ? null : text(value));
        }
        comboBox.requestLayout();
    }

    private static void applyNode(Node node) {
        if (node instanceof Labeled labeled) {
            setIfUnbound(labeled.textProperty(), text(labeled.getText()));
        }
        if (node instanceof TextInputControl input) {
            setIfUnbound(input.promptTextProperty(), text(input.getPromptText()));
        }
        if (node instanceof ComboBoxBase<?> comboBoxBase) {
            setIfUnbound(comboBoxBase.promptTextProperty(), text(comboBoxBase.getPromptText()));
        }
        if (node instanceof ComboBox<?> comboBox && comboBox.getValue() instanceof String) {
            @SuppressWarnings("unchecked")
            ComboBox<String> translatedCombo = (ComboBox<String>) comboBox;
            refreshTranslatedComboBox(translatedCombo);
        }
        if (node instanceof Axis<?> axis) {
            setIfUnbound(axis.labelProperty(), text(axis.getLabel()));
        }
        if (node instanceof MenuButton menuButton) {
            setIfUnbound(menuButton.textProperty(), text(menuButton.getText()));
            for (MenuItem item : menuButton.getItems()) {
                applyMenuItem(item);
            }
        }
        if (node instanceof TableView<?> tableView) {
            for (TableColumn<?, ?> column : tableView.getColumns()) {
                applyTableColumn(column);
            }
        }
        if (shouldSkipChildTraversal(node)) {
            return;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyNode(child);
            }
        }
    }

    private static void applyTableColumn(TableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        setIfUnbound(column.textProperty(), text(column.getText()));
        for (TableColumn<?, ?> child : column.getColumns()) {
            applyTableColumn(child);
        }
    }

    private static void applyMenuItem(MenuItem item) {
        if (item == null) {
            return;
        }
        setIfUnbound(item.textProperty(), text(item.getText()));
        if (item instanceof Menu menu) {
            for (MenuItem child : menu.getItems()) {
                applyMenuItem(child);
            }
        }
    }

    private static void setIfUnbound(Property<String> property, String value) {
        if (property == null || property.isBound()) {
            return;
        }
        property.setValue(value);
    }

    private static boolean shouldSkipChildTraversal(Node node) {
        return node instanceof ComboBoxBase<?>
                || node instanceof MenuButton
                || node instanceof TableView<?>
                || node instanceof TextInputControl;
    }

    private static String translateStructured(String value,
                                              Map<String, String> exact,
                                              List<Map.Entry<String, String>> prefixes) {
        String[] lines = value.split("\\R", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            result.append(translatePipeSegments(lines[i], exact, prefixes));
        }

        return result.toString();
    }

    private static String translatePipeSegments(String value,
                                                Map<String, String> exact,
                                                List<Map.Entry<String, String>> prefixes) {
        String[] segments = value.split("\\s+\\|\\s+", -1);
        if (segments.length == 1) {
            return translateSegment(value, exact, prefixes);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                result.append(" | ");
            }
            result.append(translateSegment(segments[i], exact, prefixes));
        }
        return result.toString();
    }

    private static String translateSegment(String value,
                                           Map<String, String> exact,
                                           List<Map.Entry<String, String>> prefixes) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String exactMatch = exact.get(value);
        if (exactMatch != null) {
            return exactMatch;
        }

        int leading = countLeadingSpaces(value);
        int trailing = countTrailingSpaces(value);
        String core = value.substring(leading, value.length() - trailing);

        String translatedCore = exact.get(core);
        if (translatedCore == null) {
            translatedCore = translateNumericSuffix(core, exact);
        }
        if (translatedCore == null) {
            translatedCore = translateKnownPrefix(core, exact, prefixes);
        }
        if (translatedCore == null) {
            translatedCore = core;
        }

        return " ".repeat(leading) + translatedCore + " ".repeat(trailing);
    }

    private static String translateNumericSuffix(String value, Map<String, String> exact) {
        int separator = value.indexOf(' ');
        if (separator <= 0 || separator >= value.length() - 1) {
            return null;
        }

        String left = value.substring(0, separator);
        String right = value.substring(separator + 1);
        if (!left.chars().allMatch(Character::isDigit)) {
            return null;
        }

        String translatedRight = exact.get(right);
        if (translatedRight == null) {
            return null;
        }
        return left + " " + translatedRight;
    }

    private static String translateKnownPrefix(String value,
                                               Map<String, String> exact,
                                               List<Map.Entry<String, String>> prefixes) {
        for (Map.Entry<String, String> entry : prefixes) {
            String source = entry.getKey();
            if (!value.startsWith(source)) {
                continue;
            }

            String suffix = value.substring(source.length());
            String translatedSuffix = translateSegment(suffix, exact, prefixes);
            return entry.getValue() + translatedSuffix;
        }
        return null;
    }

    private static int countLeadingSpaces(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
            count++;
        }
        return count;
    }

    private static int countTrailingSpaces(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(value.length() - 1 - count))) {
            count++;
        }
        return count;
    }

    private static List<Map.Entry<String, String>> sortByKeyLengthDesc(Map<String, String> values) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.comparingInt((Map.Entry<String, String> entry) -> entry.getKey().length()).reversed());
        return entries;
    }

    private static Map<String, String> createTranslations() {
        Map<String, String> values = new LinkedHashMap<>();
        addCommonTranslations(values);
        addLandingTranslations(values);
        addDashboardTranslations(values);
        addFormTranslations(values);
        addRuntimeTranslations(values);
        addServiceTranslations(values);
        addDatabaseTranslations(values);
        return values;
    }

    private static Map<String, String> createPrefixTranslations() {
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "Role: ", "Vai trò: ");
        put(values, "Account: ", "Tài khoản: ");
        put(values, "Status: ", "Trạng thái: ");
        put(values, "Category: ", "Danh mục: ");
        put(values, "Schedule: ", "Lịch: ");
        put(values, "Posted schedule: ", "Lịch đăng: ");
        put(values, "Updated at: ", "Cập nhật lúc: ");
        put(values, "Linked auction: ", "Phiên liên kết: ");
        put(values, "Linked item: ", "Mục liên kết: ");
        put(values, "Product: ", "Sản phẩm: ");
        put(values, "Remaining: ", "Còn lại: ");
        put(values, "Total transactions: ", "Tổng giao dịch: ");
        put(values, "User ID: ", "ID người dùng: ");
        put(values, "Price: ", "Giá: ");
        put(values, "At: ", "Lúc: ");
        put(values, "Auction schedule update: ", "Cập nhật lịch phiên: ");
        put(values, "Auction #", "Phiên #");
        put(values, "Bidder #", "Người đấu giá #");
        put(values, "The amount must be higher than the current price (", "Mức giá phải cao hơn giá hiện tại (");
        put(values, "The amount must be higher than the starting price (", "Mức giá phải cao hơn giá khởi điểm (");
        put(values, "AuctionScheduler error: ", "Lỗi bộ lập lịch đấu giá: ");
        put(values, "Unable to load FXML ", "Không thể tải FXML ");
        put(values, "Database configuration file not found: ", "Không tìm thấy tệp cấu hình cơ sở dữ liệu: ");
        put(values, "Invalid numeric value for ", "Giá trị số không hợp lệ cho ");
        return values;
    }

    private static void addCommonTranslations(Map<String, String> values) {
        put(values, "SETTINGS", "CÀI ĐẶT");
        put(values, "English", "Tiếng Anh");
        put(values, "Vietnamese", "Tiếng Việt");
        put(values, "Notification", "Thông báo");
        put(values, "NOTIFICATION", "THÔNG BÁO");
        put(values, "Error", "Lỗi");
        put(values, "ERROR", "LỖI");
        put(values, "Success", "Thành công");
        put(values, "SUCCESS", "THÀNH CÔNG");
        put(values, "Confirm", "Xác nhận");
        put(values, "CONFIRM", "XÁC NHẬN");
        put(values, "Cancel", "Hủy");
        put(values, "CANCEL", "HỦY");
        put(values, "Close", "Đóng");
        put(values, "CLOSE", "ĐÓNG");
        put(values, "Input", "Nhập");
        put(values, "INPUT", "NHẬP");
        put(values, "Language updated.", "Đã cập nhật ngôn ngữ.");
        put(values, "Language settings are unavailable.", "Tùy chọn ngôn ngữ hiện chưa khả dụng.");
        put(values, "LOG OUT", "ĐĂNG XUẤT");
        put(values, "SEARCH", "TÌM KIẾM");
        put(values, "Search", "Tìm kiếm");
        put(values, "Sort", "Sắp xếp");
        put(values, "Status", "Trạng thái");
        put(values, "Title", "Tiêu đề");
        put(values, "Updated", "Cập nhật");
        put(values, "ID", "MÃ");
        put(values, "Username", "Tên đăng nhập");
        put(values, "Email", "Email");
        put(values, "Role", "Vai trò");
        put(values, "Product", "Sản phẩm");
        put(values, "Product Name", "Tên sản phẩm");
        put(values, "Product Image", "Ảnh sản phẩm");
        put(values, "Category", "Danh mục");
        put(values, "Description", "Mô tả");
        put(values, "MY ACTIONS", "THAO TAC CUA TOI");
        put(values, "Add Extra Time (minutes)", "Them thoi gian (phut)");
        put(values, "Example: 15", "Vi du: 15");
        put(values, "ADD TIME", "THEM THOI GIAN");
        put(values, "TIME ADJUSTMENT", "DIEU CHINH THOI GIAN");
        put(values, "Adjust later in My Actions.", "Dieu chinh sau trong My Actions.");
        put(values, "Item", "Mặt hàng");
        put(values, "Current Bid", "Giá hiện tại");
        put(values, "Current Price", "Giá hiện tại");
        put(values, "CURRENT PRICE", "GIÁ HIỆN TẠI");
        put(values, "Highest Bid", "Giá cao nhất");
        put(values, "Duration", "Thời lượng");
        put(values, "Remaining", "Còn lại");
        put(values, "Schedule", "Lịch");
        put(values, "Posted schedule", "Lịch đăng");
        put(values, "Updated at", "Cập nhật lúc");
        put(values, "Linked auction", "Phiên liên kết");
        put(values, "Display Schedule", "Lịch hiển thị");
        put(values, "Time Window", "Khung thời gian");
        put(values, "Linked Auction", "Phiên liên kết");
        put(values, "Early Close", "Đóng sớm");
        put(values, "Linked item: -", "Mục liên kết: -");
        put(values, "Additional details", "Chi tiết bổ sung");
        put(values, "DETAILS", "CHI TIẾT");
        put(values, "Announcement Title", "Tiêu đề thông báo");
        put(values, "Announcement summary", "Tóm tắt thông báo");
        put(values, "Not linked", "Chưa liên kết");
        put(values, "No linked auction", "Không có phiên liên kết");
        put(values, "LIVE", "TRỰC TIẾP");
        put(values, "OPENING SOON", "SẮP MỞ");
        put(values, "RUNNING", "ĐANG DIỄN RA");
        put(values, "FINISHED", "ĐÃ KẾT THÚC");
        put(values, "CANCELLED", "ĐÃ HỦY");
        put(values, "PAID", "ĐÃ THANH TOÁN");
        put(values, "ACTIVE", "HOẠT ĐỘNG");
        put(values, "BLOCKED", "ĐÃ KHÓA");
        put(values, "ACCEPTED", "ĐƯỢC CHẤP NHẬN");
        put(values, "Admin", "Quản trị viên");
        put(values, "Seller", "Người bán");
        put(values, "Bidder", "Người đấu giá");
        put(values, "Admin CMS", "CMS quản trị");
        put(values, "ADMIN", "QUẢN TRỊ");
        put(values, "SELLER", "NGƯỜI BÁN");
        put(values, "BIDDER", "NGƯỜI ĐẤU GIÁ");
        put(values, "All", "Tất cả");
        put(values, "Running", "Đang diễn ra");
        put(values, "Opening Soon", "Sắp mở");
        put(values, "Finished", "Đã kết thúc");
        put(values, "Default", "Mặc định");
        put(values, "Product Name A-Z", "Tên sản phẩm A-Z");
        put(values, "Highest Bid Descending", "Giá cao nhất giảm dần");
        put(values, "Ending Soon", "Sắp kết thúc");
        put(values, "Category A-Z", "Danh mục A-Z");
        put(values, "Not active", "Chưa kích hoạt");
        put(values, "Not started", "Chưa bắt đầu");
        put(values, "Ended", "Đã kết thúc");
        put(values, "counts left", "lượt đếm còn lại");
        put(values, "transactions", "giao dịch");
        put(values, "bid", "lượt đấu giá");
        put(values, "min", "phút");
        put(values, "minutes", "phút");
        put(values, "sec", "giây");
        put(values, "Auction", "Phiên");
        put(values, "Auction #", "Phiên #");
        put(values, "Auction schedule update", "Cập nhật lịch phiên");
        put(values, "Bidder #", "Người đấu giá #");
        put(values, "Account:", "Tài khoản:");
        put(values, "Account: -", "Tài khoản: -");
        put(values, "Seller ID", "ID người bán");
        put(values, "User ID", "ID người dùng");
        put(values, "Price", "Giá");
        put(values, "At", "Lúc");
        put(values, "Total transactions", "Tổng giao dịch");
        put(values, "No bid transactions yet.", "Chưa có giao dịch đấu giá nào.");
        put(values, "New bid activity will appear here immediately.", "Hoạt động ra giá mới sẽ xuất hiện ngay tại đây.");
        put(values, "This product does not have a detailed description yet.", "Sản phẩm này chưa có mô tả chi tiết.");
        put(values, "No leading bidder yet", "Chưa có người dẫn đầu");
        put(values, "You are leading", "Bạn đang dẫn đầu");
        put(values, "YOU ARE LEADING", "BẠN ĐANG DẪN ĐẦU");
        put(values, "LEADING BIDDER", "NGƯỜI DẪN ĐẦU");
        put(values, "BID COUNT", "SỐ LƯỢT ĐẤU GIÁ");
        put(values, "COUNTDOWN", "ĐẾM NGƯỢC");
        put(values, "SCHEDULE", "LỊCH");
        put(values, "BID HISTORY", "LỊCH SỬ ĐẤU GIÁ");
        put(values, "NO AUCTION SELECTED", "CHƯA CHỌN PHIÊN ĐẤU GIÁ");
        put(values, "Realtime", "Thời gian thực");
        put(values, "REALTIME", "THỜI GIAN THỰC");
        put(values, "SECURE", "BẢO MẬT");
        put(values, "1s", "1 giây");
        put(values, "Instruction text", "Nội dung hướng dẫn");
        put(values, "Error message", "Thông báo lỗi");
        put(values, "Notification content", "Nội dung thông báo");
        put(values, "0 transactions", "0 giao dịch");
        put(values, "30 minutes", "30 phút");
        put(values, "Enter value", "Nhập giá trị");
        put(values, "A short summary will appear here.", "Phần tóm tắt ngắn sẽ hiển thị tại đây.");
        put(values, "Additional details and instructions will appear here.", "Chi tiết và hướng dẫn bổ sung sẽ hiển thị tại đây.");
        put(values, "Bid status information will appear here.", "Thông tin trạng thái ra giá sẽ hiển thị tại đây.");
        put(values, "Additional details, notes, instructions...", "Chi tiết, ghi chú, hướng dẫn bổ sung...");
        put(values, "Short summary displayed on the homepage", "Tóm tắt ngắn hiển thị trên trang chủ");
        put(values, "Auction schedule information shown on the homepage", "Thông tin lịch phiên hiển thị trên trang chủ");
        put(values, "Example: 1500000", "Ví dụ: 1500000");
        put(values, "Example: Electronics", "Ví dụ: Điện tử");
        put(values, "A", "A");
        put(values, "AD", "QT");
        put(values, "24/7", "24/7");
    }

    private static void addLandingTranslations(Map<String, String> values) {
        put(values, "PRODUCT AUCTION PLATFORM", "NỀN TẢNG ĐẤU GIÁ SẢN PHẨM");
        put(values, "PREMIUM AUCTION APP", "ỨNG DỤNG ĐẤU GIÁ CAO CẤP");
        put(values, "FAST OVERVIEW", "TỔNG QUAN NHANH");
        put(values, "LIVE AUCTION EXPERIENCE", "TRẢI NGHIỆM ĐẤU GIÁ TRỰC TIẾP");
        put(values, "AUCTION HOUSE STUDIO", "KHÔNG GIAN NHÀ ĐẤU GIÁ");
        put(values, "AUCTION HOUSE", "NHÀ ĐẤU GIÁ");
        put(values, "Online auctions for bidders, sellers, and admins", "Đấu giá trực tuyến cho người đấu giá, người bán và quản trị viên");
        put(values, "Browse live auctions, see upcoming listings, and follow admin updates from one homepage.", "Theo dõi phiên đang diễn ra, xem lịch sắp mở và cập nhật quản trị ngay trên một trang chủ.");
        put(values, "Log in to bid, sell, or manage the platform.", "Đăng nhập để đấu giá, bán hàng hoặc quản lý nền tảng.");
        put(values, "Live stats, featured auctions, and homepage updates stay in one place", "Thống kê trực tiếp, phiên nổi bật và cập nhật trang chủ được gom trong một nơi");
        put(values, "The homepage now opens straight to the information users actually need.", "Trang chủ giờ đi thẳng vào những thông tin người dùng thực sự cần.");
        put(values, "Counts and featured content refresh automatically without crowding the screen.", "Số liệu và nội dung nổi bật tự làm mới mà vẫn giữ giao diện gọn gàng.");
        put(values, "FEATURED AUCTIONS AND HOMEPAGE UPDATES", "PHIÊN NỔI BẬT VÀ CẬP NHẬT TRANG CHỦ");
        put(values, "Live and upcoming auctions are listed beside homepage announcements.", "Các phiên đang diễn ra và sắp mở được hiển thị cạnh thông báo trang chủ.");
        put(values, "AUCTION SPOTLIGHT", "PHIÊN NỔI BẬT");
        put(values, "HOMEPAGE UPDATES", "CẬP NHẬT TRANG CHỦ");
        put(values, "LIVE AUCTIONS", "PHIÊN ĐANG DIỄN RA");
        put(values, "ADMIN UPDATES", "CẬP NHẬT TỪ QUẢN TRỊ");
        put(values, "No auctions available yet", "Chưa có phiên đấu giá nào.");
        put(values, "When a seller creates a new auction or one begins, this list updates automatically.", "Khi người bán tạo phiên mới hoặc phiên bắt đầu, danh sách này sẽ tự cập nhật.");
        put(values, "No admin announcements yet", "Chưa có thông báo quản trị.");
        put(values, "Announcements, auction schedules, and featured items will appear here after admins publish updates.", "Thông báo, lịch phiên và nội dung nổi bật sẽ xuất hiện tại đây sau khi quản trị đăng tải.");
        put(values, "Information will be updated later", "Thông tin sẽ được cập nhật sau.");
        put(values, "Content will be updated later", "Nội dung sẽ được cập nhật sau.");
    }

    private static void addDashboardTranslations(Map<String, String> values) {
        put(values, "Seller Dashboard", "Bảng điều khiển người bán");
        put(values, "Bidder Dashboard", "Bảng điều khiển người đấu giá");
        put(values, "Admin Dashboard", "Bảng điều khiển quản trị");
        put(values, "Homepage Content Manager", "Quản lý nội dung trang chủ");
        put(values, "Create, edit, and monitor auctions from one workspace.", "Tạo, chỉnh sửa và theo dõi phiên đấu giá trong một khu làm việc.");
        put(values, "Create, edit, and monitor auctions from one screen.", "Tạo, chỉnh sửa và theo dõi phiên đấu giá trên một màn hình.");
        put(values, "Use the form on the left and the live preview on the right.", "Dùng biểu mẫu bên trái và xem trước trực tiếp bên phải.");
        put(values, "Bidder Workspace", "Không gian người đấu giá");
        put(values, "Track auctions, review live details, and bid without switching screens.", "Theo dõi phiên, xem chi tiết trực tiếp và đặt giá mà không cần đổi màn hình.");
        put(values, "Search auctions, inspect details, and place bids quickly.", "Tìm kiếm phiên, xem chi tiết và đặt giá nhanh chóng.");
        put(values, "Monitor users, auctions, and system activity from one dashboard.", "Theo dõi người dùng, phiên đấu giá và hoạt động hệ thống trên một bảng điều khiển.");
        put(values, "Create and schedule homepage announcements from one screen.", "Tạo và lên lịch thông báo trang chủ trên một màn hình.");
        put(values, "Create and schedule homepage announcements.", "Tạo và lên lịch thông báo trang chủ.");
        put(values, "Edit content, preview it, and manage published announcements in one place.", "Chỉnh sửa nội dung, xem trước và quản lý thông báo đã đăng trong cùng một nơi.");
        put(values, "Monitor users, auctions, and homepage activity from one control panel.", "Theo dõi người dùng, phiên đấu giá và hoạt động trang chủ trên một bảng điều khiển.");
        put(values, "Use the tables below for account actions, homepage access, and auction control.", "Dùng các bảng bên dưới để thao tác tài khoản, vào trang chủ và điều khiển phiên đấu giá.");

        put(values, "MODULES", "MÔ-ĐUN");
        put(values, "USER SESSION", "PHIÊN NGƯỜI DÙNG");
        put(values, "Auction Studio", "Xưởng phiên đấu giá");
        put(values, "My Auctions", "Phiên của tôi");
        put(values, "Bid History", "Lịch sử đấu giá");
        put(values, "Overview", "Tổng quan");
        put(values, "Users", "Người dùng");
        put(values, "Auctions", "Phiên đấu giá");
        put(values, "Homepage", "Trang chủ");
        put(values, "Announcements", "Thông báo");
        put(values, "Homepage Auctions", "Phiên cho trang chủ");
        put(values, "Homepage CMS", "CMS trang chủ");
        put(values, "SELLER STUDIO", "KHU NGƯỜI BÁN");
        put(values, "LIVE BIDDING FLOOR", "SÀN ĐẤU GIÁ TRỰC TIẾP");
        put(values, "HOMEPAGE CMS", "CMS TRANG CHỦ");
        put(values, "SYSTEM CONTROL CENTER", "TRUNG TÂM ĐIỀU KHIỂN HỆ THỐNG");

        put(values, "TOTAL AUCTIONS", "TỔNG SỐ PHIÊN");
        put(values, "OPEN / OPENING SOON", "ĐANG MỞ / SẮP MỞ");
        put(values, "RUNNING AUCTIONS", "PHIÊN ĐANG DIỄN RA");
        put(values, "ENDING SOON", "SẮP KẾT THÚC");
        put(values, "TOTAL USERS", "TỔNG NGƯỜI DÙNG");
        put(values, "TOTAL BIDS", "TỔNG LƯỢT ĐẤU GIÁ");
        put(values, "TOTAL ANNOUNCEMENTS", "TỔNG THÔNG BÁO");
        put(values, "LINKED AUCTIONS", "PHIÊN LIÊN KẾT");
        put(values, "VISIBLE AUCTIONS", "PHIÊN HIỂN THỊ");

        put(values, "CREATE / EDIT AUCTION STUDIO", "TẠO / CHỈNH SỬA PHIÊN ĐẤU GIÁ");
        put(values, "MY AUCTIONS", "PHIÊN CỦA TÔI");
        put(values, "AUCTION LIST", "DANH SÁCH PHIÊN ĐẤU GIÁ");
        put(values, "PRICE OVER TIME", "BIẾN ĐỘNG GIÁ THEO THỜI GIAN");
        put(values, "QUICK BID", "RA GIÁ NHANH");
        put(values, "LIVE BID FEED", "DÒNG RA GIÁ TRỰC TIẾP");
        put(values, "AUCTION DETAIL", "CHI TIẾT PHIÊN");
        put(values, "LIVE PREVIEW", "XEM TRƯỚC TRỰC TIẾP");
        put(values, "CREATE / EDIT HOMEPAGE ANNOUNCEMENT", "TẠO / CHỈNH SỬA THÔNG BÁO TRANG CHỦ");
        put(values, "CURRENT ANNOUNCEMENTS", "THÔNG BÁO HIỆN TẠI");
        put(values, "AUCTIONS READY FOR HOMEPAGE", "PHIÊN SẴN SÀNG CHO TRANG CHỦ");
        put(values, "NAVIGATION", "ĐIỀU HƯỚNG");
        put(values, "AUCTION OPERATIONS", "THAO TÁC PHIÊN ĐẤU GIÁ");
        put(values, "AUCTION OVERVIEW", "TỔNG QUAN PHIÊN");
        put(values, "QUICK INFO", "THÔNG TIN NHANH");
        put(values, "USER LIST", "DANH SÁCH NGƯỜI DÙNG");
        put(values, "CHANGE PASSWORD", "ĐỔI MẬT KHẨU");

        put(values, "EDIT SELECTED", "SỬA MỤC ĐÃ CHỌN");
        put(values, "DELETE / CANCEL AUCTION", "XÓA / HỦY PHIÊN");
        put(values, "CLOSE EARLY", "ĐÓNG SỚM");
        put(values, "PLACE BID NOW", "ĐẶT GIÁ NGAY");
        put(values, "REFRESH USERS", "LÀM MỚI NGƯỜI DÙNG");
        put(values, "REFRESH AUCTIONS", "LÀM MỚI PHIÊN");
        put(values, "LOCK / UNLOCK", "KHÓA / MỞ KHÓA");
        put(values, "START 3-COUNTDOWN", "BẮT ĐẦU ĐẾM 3");
        put(values, "CANCEL COUNTDOWN", "HỦY ĐẾM NGƯỢC");
        put(values, "MANAGE HOMEPAGE", "QUẢN LÝ TRANG CHỦ");
        put(values, "EDIT ANNOUNCEMENT", "SỬA THÔNG BÁO");
        put(values, "DELETE ANNOUNCEMENT", "XÓA THÔNG BÁO");
        put(values, "BACK TO ADMIN DASHBOARD", "QUAY LẠI BẢNG ĐIỀU KHIỂN");
        put(values, "USE SELECTED AUCTION SCHEDULE", "DÙNG LỊCH CỦA PHIÊN ĐÃ CHỌN");
        put(values, "CLEAR FORM", "XÓA BIỂU MẪU");
        put(values, "PUBLISH TO HOMEPAGE", "ĐĂNG LÊN TRANG CHỦ");
        put(values, "REFRESH", "LÀM MỚI");
        put(values, "CHOOSE IMAGE", "CHỌN ẢNH");
        put(values, "CREATE NEW", "TẠO MỚI");
        put(values, "CANCEL UPDATE", "HỦY CẬP NHẬT");
        put(values, "Auction Duration (minutes)", "Thời lượng phiên (phút)");
        put(values, "Starting Price", "Giá khởi điểm");
        put(values, "STARTING PRICE", "GIÁ KHỞI ĐIỂM");
        put(values, "Enter product name", "Nhập tên sản phẩm");
        put(values, "Detailed description of the product so bidders understand it better before placing a bid", "Mô tả chi tiết sản phẩm để người đấu giá hiểu rõ hơn trước khi ra giá");
        put(values, "Local file path or product image URL", "Đường dẫn ảnh cục bộ hoặc URL ảnh sản phẩm");
        put(values, "Use .png, .jpg, or .jpeg for clearer previews.", "Dùng .png, .jpg hoặc .jpeg để xem trước rõ hơn.");
        put(values, "Choose product image", "Chọn ảnh sản phẩm");
        put(values, "Image Files", "Tệp hình ảnh");
        put(values, "The description updates instantly as the seller types.", "Mô tả sẽ cập nhật ngay khi người bán nhập.");
        put(values, "Creating a new auction", "Đang tạo phiên mới");
        put(values, "Editing auction", "Đang chỉnh sửa phiên");
        put(values, "Invalid price", "Giá không hợp lệ");
        put(values, "Select an auction to view details", "Chọn một phiên để xem chi tiết");
        put(values, "Select an auction to view details.", "Chọn một phiên để xem chi tiết.");
        put(values, "The product description will appear here.", "Mô tả sản phẩm sẽ hiển thị tại đây.");
        put(values, "Search by product name or category", "Tìm theo tên sản phẩm hoặc danh mục");
        put(values, "STATUS FILTER", "BỘ LỌC TRẠNG THÁI");
        put(values, "STATUS FORM", "TR\u1ea0NG TH\u00c1I BI\u1ec2U M\u1eaaU");
        put(values, "Auction Extension (Anti-sniping)", "Gia h\u1ea1n phi\u00ean (Anti-sniping)");
        put(values, "Enable automatic extension near closing time", "B\u1eadt t\u1ef1 \u0111\u1ed9ng gia h\u1ea1n khi phi\u00ean s\u1eafp k\u1ebft th\u00fac");
        put(values, "Threshold (sec)", "Ng\u01b0\u1ee1ng (gi\u00e2y)");
        put(values, "Extend By (sec)", "Gia h\u1ea1n th\u00eam (gi\u00e2y)");
        put(values, "Max Extensions", "S\u1ed1 l\u1ea7n gia h\u1ea1n t\u1ed1i \u0111a");
        put(values, "AUTO EXTEND", "T\u1ef0 \u0110\u1ed8NG GIA H\u1ea0N");
        put(values, "Example: last 30 seconds extend by 60 seconds, up to 5 times.", "V\u00ed d\u1ee5: 30 gi\u00e2y cu\u1ed1i s\u1ebd gia h\u1ea1n th\u00eam 60 gi\u00e2y, t\u1ed1i \u0111a 5 l\u1ea7n.");
        put(values, "Invalid extension setup", "C\u1ea5u h\u00ecnh gia h\u1ea1n kh\u00f4ng h\u1ee3p l\u1ec7");
        put(values, "Last", "Cu\u1ed1i");
        put(values, "Max", "T\u1ed1i \u0111a");
        put(values, "times", "l\u1ea7n");
        put(values, "Disabled", "T\u1eaft");
    }

    private static void addFormTranslations(Map<String, String> values) {
        put(values, "LOG IN", "ĐĂNG NHẬP");
        put(values, "REGISTER", "ĐĂNG KÝ");
        put(values, "CREATE NEW ACCOUNT", "TẠO TÀI KHOẢN MỚI");
        put(values, "ACCOUNT LOGIN", "ĐĂNG NHẬP TÀI KHOẢN");
        put(values, "ACCOUNT REGISTRATION", "ĐĂNG KÝ TÀI KHOẢN");
        put(values, "BACK TO LOGIN", "QUAY LẠI ĐĂNG NHẬP");
        put(values, "COMPLETE REGISTRATION", "HOÀN TẤT ĐĂNG KÝ");
        put(values, "Password", "Mật khẩu");
        put(values, "Full Name", "Họ và tên");
        put(values, "Confirm Password", "Xác nhận mật khẩu");
        put(values, "Enter your username", "Nhập tên đăng nhập");
        put(values, "Enter your password", "Nhập mật khẩu");
        put(values, "Enter username", "Nhập tên đăng nhập");
        put(values, "Enter your full name", "Nhập họ và tên");
        put(values, "Enter email address", "Nhập địa chỉ email");
        put(values, "Confirm your password", "Nhập lại mật khẩu");
        put(values, "Remember this login session", "Ghi nhớ phiên đăng nhập này");
        put(values, "Forgot password?", "Quên mật khẩu?");
        put(values, "Current Password", "Mật khẩu hiện tại");
        put(values, "New Password", "Mật khẩu mới");
        put(values, "Enter current password", "Nhập mật khẩu hiện tại");
        put(values, "Enter new password", "Nhập mật khẩu mới");
        put(values, "Use your existing username and password to enter the auction workspace.", "Dùng tài khoản hiện có để vào hệ thống đấu giá.");
        put(values, "Complete all required information to join the online auction system.", "Hoàn tất đầy đủ thông tin để tham gia hệ thống đấu giá trực tuyến.");
        put(values, "Create a new auction account", "Tạo tài khoản đấu giá mới");
        put(values, "Register once, then sign in as a bidder, seller, or admin.", "Đăng ký một lần rồi đăng nhập với vai trò người đấu giá, người bán hoặc quản trị.");
        put(values, "Sign in to the auction platform", "Đăng nhập vào nền tảng đấu giá");
        put(values, "Access bidder, seller, or admin workspaces from one account system.", "Truy cập khu người đấu giá, người bán hoặc quản trị từ một hệ thống tài khoản.");
        put(values, "Live Validation", "Kiểm tra trực tiếp");
        put(values, "Email, password, and password confirmation are validated while you type.", "Email, mật khẩu và xác nhận mật khẩu được kiểm tra ngay khi bạn nhập.");
        put(values, "A valid password must have at least 6 characters and include both letters and numbers.", "Mật khẩu hợp lệ phải có ít nhất 6 ký tự và gồm cả chữ lẫn số.");
        put(values, "A valid password must be at least 6 characters long and include letters and numbers.", "Mật khẩu hợp lệ phải có ít nhất 6 ký tự và gồm cả chữ lẫn số.");
        put(values, "Password must be at least 6 characters and include letters and numbers", "Mật khẩu phải có ít nhất 6 ký tự và gồm cả chữ lẫn số");
        put(values, "Password must be at least 6 characters long and include letters and numbers.", "Mật khẩu phải có ít nhất 6 ký tự và gồm cả chữ lẫn số.");
        put(values, "Password looks good. You can continue registration.", "Mật khẩu hợp lệ. Bạn có thể tiếp tục đăng ký.");
        put(values, "Password must be at least 6 characters long with letters and numbers. Confirmation must match.", "Mật khẩu phải có ít nhất 6 ký tự, gồm chữ và số, đồng thời phần xác nhận phải khớp.");
        put(values, "Please review the registration information.", "Vui lòng kiểm tra lại thông tin đăng ký.");
        put(values, "Invalid email.", "Email không hợp lệ.");
        put(values, "Password confirmation does not match.", "Xác nhận mật khẩu không khớp.");
        put(values, "Invalid role.", "Vai trò không hợp lệ.");
        put(values, "Please fill in all required information.", "Vui lòng điền đầy đủ thông tin bắt buộc.");
    }

    private static void addRuntimeTranslations(Map<String, String> values) {
        put(values, "SAVE CHANGES", "LƯU THAY ĐỔI");
        put(values, "UPDATE", "CẬP NHẬT");
        put(values, "Login failed", "Đăng nhập thất bại");
        put(values, "LOGIN FAILED", "ĐĂNG NHẬP THẤT BẠI");
        put(values, "Login failed.", "Đăng nhập thất bại.");
        put(values, "AuthController has not been assigned to RegisterViewController.", "AuthController chưa được gán cho RegisterViewController.");
        put(values, "Registration completed successfully. Please log in.", "Đăng ký thành công. Vui lòng đăng nhập.");
        put(values, "Unable to complete registration.", "Không thể hoàn tất đăng ký lúc này.");
        put(values, "AuthController has not been assigned to LoginViewController.", "AuthController chưa được gán cho LoginViewController.");
        put(values, "Please enter both username and password.", "Vui lòng nhập cả tên đăng nhập và mật khẩu.");
        put(values, "A dedicated password recovery flow is not available in this version.", "Phiên bản này chưa có luồng khôi phục mật khẩu riêng.");
        put(values, "Please contact an admin for password assistance.", "Vui lòng liên hệ quản trị viên để được hỗ trợ mật khẩu.");
        put(values, "Not enough information to change the password.", "Không đủ thông tin để đổi mật khẩu.");
        put(values, "Please enter both the current password and the new password.", "Vui lòng nhập cả mật khẩu hiện tại và mật khẩu mới.");
        put(values, "Password changed successfully.", "Đổi mật khẩu thành công.");
        put(values, "Unable to change the password right now.", "Hiện không thể đổi mật khẩu.");
        put(values, "Unable to load admin data.", "Không thể tải dữ liệu quản trị.");
        put(values, "Unable to load auction data.", "Không thể tải dữ liệu phiên đấu giá.");
        put(values, "Unable to load homepage data", "Không thể tải dữ liệu trang chủ");
        put(values, "This screen has not been connected to AuthFrame yet.", "Màn hình này chưa được kết nối với AuthFrame.");
        put(values, "Current admin information is unavailable.", "Thông tin quản trị hiện không khả dụng.");
        put(values, "HomepageController has not been assigned to the admin screen.", "HomepageController chưa được gán cho màn hình quản trị.");
        put(values, "Please select a running auction.", "Vui lòng chọn một phiên đang diễn ra.");
        put(values, "The 3-count early close countdown has started. If no new bid arrives, the auction will close early.", "Đã bắt đầu đếm ngược đóng sớm 3 nhịp. Nếu không có giá mới, phiên sẽ đóng sớm.");
        put(values, "Please select an auction with an active early-close countdown.", "Vui lòng chọn một phiên đang có đếm ngược đóng sớm.");
        put(values, "The early-close countdown has been cancelled.", "Đã hủy đếm ngược đóng sớm.");
        put(values, "AuthController has not been assigned to the admin screen.", "AuthController chưa được gán cho màn hình quản trị.");
        put(values, "Current user information is unavailable.", "Thông tin người dùng hiện không khả dụng.");
        put(values, "Please select an account.", "Vui lòng chọn một tài khoản.");
        put(values, "Account status updated successfully.", "Đã cập nhật trạng thái tài khoản thành công.");
        put(values, "Current user is unavailable.", "Người dùng hiện không khả dụng.");
        put(values, "The change-password action is prepared. Connect this controller to AuthFrame when integrating.", "Chức năng đổi mật khẩu đã sẵn sàng. Hãy kết nối controller này với AuthFrame khi tích hợp.");
        put(values, "The logout action is prepared. Connect this controller to AuthFrame when integrating.", "Chức năng đăng xuất đã sẵn sàng. Hãy kết nối controller này với AuthFrame khi tích hợp.");
        put(values, "Select a user to view a summary here.", "Chọn một người dùng để xem tóm tắt tại đây.");
        put(values, "Select an auction to track its countdown and status here.", "Chọn một phiên để theo dõi đếm ngược và trạng thái tại đây.");
        put(values, "AuctionController has not been assigned to the admin screen.", "AuctionController chưa được gán cho màn hình quản trị.");
        put(values, "Unable to complete this action right now.", "Hiện không thể hoàn tất thao tác này.");
        put(values, "Required controllers have not been assigned to the homepage management screen.", "Các controller cần thiết chưa được gán cho màn hình quản lý trang chủ.");
        put(values, "Please select an announcement to edit.", "Vui lòng chọn một thông báo để chỉnh sửa.");
        put(values, "Please select an announcement to delete.", "Vui lòng chọn một thông báo để xóa.");
        put(values, "Are you sure you want to remove this announcement from the homepage?", "Bạn có chắc muốn gỡ thông báo này khỏi trang chủ không?");
        put(values, "Homepage announcement deleted successfully.", "Đã xóa thông báo trang chủ thành công.");
        put(values, "Please select an auction to use its schedule.", "Vui lòng chọn một phiên để dùng lịch của nó.");
        put(values, "Homepage announcement updated successfully.", "Đã cập nhật thông báo trang chủ thành công.");
        put(values, "Bid placement is not ready.", "Chức năng đặt giá hiện chưa sẵn sàng.");
        put(values, "Please select an auction.", "Vui lòng chọn một phiên.");
        put(values, "Please enter a bid amount.", "Vui lòng nhập mức giá đấu.");
        put(values, "Please enter a bid amount before placing a bid.", "Vui lòng nhập mức giá trước khi đặt giá.");
        put(values, "Submitting your bid...", "Đang gửi giá đấu của bạn...");
        put(values, "Bid placed successfully. Refreshing the selected auction.", "Đặt giá thành công. Đang làm mới phiên đã chọn.");
        put(values, "Bid placed successfully.", "Đặt giá thành công.");
        put(values, "Unable to place a bid right now.", "Hiện không thể đặt giá.");
        put(values, "Invalid amount.", "Số tiền không hợp lệ.");
        put(values, "AuctionController has not been assigned to the bidder screen.", "AuctionController chưa được gán cho màn hình người đấu giá.");
        put(values, "Connect this controller to AuthFrame to open bid history using FXML.", "Hãy kết nối controller này với AuthFrame để mở lịch sử đấu giá bằng FXML.");
        put(values, "You have just been outbid in this auction.", "Bạn vừa bị vượt giá ở phiên này.");
        put(values, "Outbid", "Bị vượt giá");
        put(values, "You have just been outbid in the auction you are watching.", "Bạn vừa bị vượt giá trong phiên đang theo dõi.");
        put(values, "Leading", "Đang dẫn đầu");
        put(values, "You are currently leading.", "Bạn hiện đang dẫn đầu.");
        put(values, "You currently have the highest bid in this auction.", "Bạn hiện đang có mức giá cao nhất ở phiên này.");
        put(values, "AuctionController has not been assigned to the seller screen.", "AuctionController chưa được gán cho màn hình người bán.");
        put(values, "Current seller information is unavailable.", "Thông tin người bán hiện không khả dụng.");
        put(values, "Auction saved successfully.", "Đã lưu phiên đấu giá thành công.");
        put(values, "Invalid starting price.", "Giá khởi điểm không hợp lệ.");
        put(values, "Auction deletion is not ready.", "Chức năng xóa phiên hiện chưa sẵn sàng.");
        put(values, "Are you sure you want to delete or cancel this auction?", "Bạn có chắc muốn xóa hoặc hủy phiên này không?");
        put(values, "Auction deleted or cancelled successfully.", "\u0110\u00e3 x\u00f3a ho\u1eb7c h\u1ee7y phi\u00ean th\u00e0nh c\u00f4ng.");
        put(values, "Auction closing is not ready.", "Ch\u1ee9c n\u0103ng \u0111\u00f3ng phi\u00ean hi\u1ec7n ch\u01b0a s\u1eb5n s\u00e0ng.");
        put(values, "Auction closed successfully.", "\u0110\u00e3 \u0111\u00f3ng phi\u00ean th\u00e0nh c\u00f4ng.");
        put(values, "Auction time adjustment is not ready.", "Ch\u1ee9c n\u0103ng \u0111i\u1ec1u ch\u1ec9nh th\u1eddi gian phi\u00ean hi\u1ec7n ch\u01b0a s\u1eb5n s\u00e0ng.");
        put(values, "Additional minutes must be a positive integer.", "S\u1ed1 ph\u00fat c\u1ea7n th\u00eam ph\u1ea3i l\u00e0 s\u1ed1 nguy\u00ean d\u01b0\u01a1ng.");
        put(values, "Auction time extended successfully.", "\u0110\u00e3 th\u00eam th\u1eddi gian cho phi\u00ean th\u00e0nh c\u00f4ng.");
        put(values, "Extension threshold must be a positive number of seconds.", "Ng\u01b0\u1ee1ng gia h\u1ea1n ph\u1ea3i l\u00e0 s\u1ed1 gi\u00e2y d\u01b0\u01a1ng.");
        put(values, "Extension duration must be a positive number of seconds.", "Th\u1eddi gian gia h\u1ea1n ph\u1ea3i l\u00e0 s\u1ed1 gi\u00e2y d\u01b0\u01a1ng.");
        put(values, "Maximum extension count must be a positive integer.", "S\u1ed1 l\u1ea7n gia h\u1ea1n t\u1ed1i \u0111a ph\u1ea3i l\u00e0 s\u1ed1 nguy\u00ean d\u01b0\u01a1ng.");
        put(values, "Only admins can issue early-close commands.", "Ch\u1ec9 qu\u1ea3n tr\u1ecb vi\u00ean m\u1edbi \u0111\u01b0\u1ee3c ra l\u1ec7nh \u0111\u00f3ng s\u1edbm.");
        put(values, "Only admins can cancel early-close commands.", "Chỉ quản trị viên mới được hủy lệnh đóng sớm.");
        put(values, "Only admins can publish announcements to the homepage.", "Chỉ quản trị viên mới được đăng thông báo lên trang chủ.");
        put(values, "Only admins can delete homepage announcements.", "Chỉ quản trị viên mới được xóa thông báo trang chủ.");
    }

    private static void addServiceTranslations(Map<String, String> values) {
        put(values, "Account for lock or unlock was not found.", "Không tìm thấy tài khoản để khóa hoặc mở khóa.");
        put(values, "Announcement summary cannot be empty.", "Tóm tắt thông báo không được để trống.");
        put(values, "Announcement title cannot be empty.", "Tiêu đề thông báo không được để trống.");
        put(values, "Auction item not found.", "Không tìm thấy mặt hàng đấu giá.");
        put(values, "Auction not found.", "Không tìm thấy phiên đấu giá.");
        put(values, "Auction schedule information cannot be empty.", "Thông tin lịch phiên không được để trống.");
        put(values, "Cannot create an expired auction.", "Không thể tạo phiên đã hết hạn.");
        put(values, "Current password is incorrect.", "Mật khẩu hiện tại không đúng.");
        put(values, "Early-close countdown is only available while the auction is RUNNING.", "Đếm ngược đóng sớm chỉ khả dụng khi phiên đang RUNNING.");
        put(values, "Email already exists.", "Email đã tồn tại.");
        put(values, "Full name cannot be empty.", "Họ và tên không được để trống.");
        put(values, "Homepage announcement not found.", "Không tìm thấy thông báo trang chủ.");
        put(values, "Incorrect username or password.", "Tên đăng nhập hoặc mật khẩu không đúng.");
        put(values, "Invalid password. It must be at least 6 characters long and include letters and numbers.", "Mật khẩu không hợp lệ. Mật khẩu phải có ít nhất 6 ký tự và gồm cả chữ lẫn số.");
        put(values, "Invalid username. It must be 3 to 20 characters long and cannot be empty.", "Tên đăng nhập không hợp lệ. Tên đăng nhập phải dài từ 3 đến 20 ký tự và không được để trống.");
        put(values, "Item not found.", "Không tìm thấy mục.");
        put(values, "New password must be at least 6 characters and include letters and numbers.", "Mật khẩu mới phải có ít nhất 6 ký tự và gồm cả chữ lẫn số.");
        put(values, "Only admins can lock or unlock accounts.", "Chỉ quản trị viên mới được khóa hoặc mở khóa tài khoản.");
        put(values, "Only the creator can delete or cancel this item.", "Chỉ người tạo mới được xóa hoặc hủy mục này.");
        put(values, "Only the creator can edit this item.", "Ch\u1ec9 ng\u01b0\u1eddi t\u1ea1o m\u1edbi \u0111\u01b0\u1ee3c ch\u1ec9nh s\u1eeda m\u1ee5c n\u00e0y.");
        put(values, "Please enter your username and password.", "Vui l\u00f2ng nh\u1eadp t\u00ean \u0111\u0103ng nh\u1eadp v\u00e0 m\u1eadt kh\u1ea9u.");
        put(values, "Product name cannot be empty.", "T\u00ean s\u1ea3n ph\u1ea9m kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng.");
        put(values, "Start time must be earlier than end time.", "Th\u1eddi gian b\u1eaft \u0111\u1ea7u ph\u1ea3i s\u1edbm h\u01a1n th\u1eddi gian k\u1ebft th\u00fac.");
        put(values, "Starting price must be greater than 0.", "Gi\u00e1 kh\u1edfi \u0111i\u1ec3m ph\u1ea3i l\u1edbn h\u01a1n 0.");
        put(values, "Additional minutes must be greater than 0.", "S\u1ed1 ph\u00fat c\u1ea7n th\u00eam ph\u1ea3i l\u1edbn h\u01a1n 0.");
        put(values, "Extension threshold must be greater than 0 seconds.", "Ng\u01b0\u1ee1ng gia h\u1ea1n ph\u1ea3i l\u1edbn h\u01a1n 0 gi\u00e2y.");
        put(values, "Extension duration must be greater than 0 seconds.", "Th\u1eddi gian gia h\u1ea1n ph\u1ea3i l\u1edbn h\u01a1n 0 gi\u00e2y.");
        put(values, "Maximum extension count must be greater than 0.", "S\u1ed1 l\u1ea7n gia h\u1ea1n t\u1ed1i \u0111a ph\u1ea3i l\u1edbn h\u01a1n 0.");
        put(values, "Extension duration should be greater than or equal to the threshold window.", "Th\u1eddi gian gia h\u1ea1n n\u00ean l\u1edbn h\u01a1n ho\u1eb7c b\u1eb1ng ng\u01b0\u1ee1ng ki\u1ec3m tra.");
        put(values, "The auction has already ended or was cancelled.", "Phi\u00ean \u0111\u1ea5u gi\u00e1 \u0111\u00e3 k\u1ebft th\u00fac ho\u1eb7c \u0111\u00e3 b\u1ecb h\u1ee7y.");
        put(values, "The auction is not currently running.", "Phiên đấu giá hiện không chạy.");
        put(values, "The current time is not valid for bidding.", "Thời điểm hiện tại không hợp lệ để đấu giá.");
        put(values, "This auction can only be extended while it is OPEN or RUNNING.", "Phiên này chỉ có thể thêm thời gian khi đang OPEN hoặc RUNNING.");
        put(values, "This auction has not activated the early-close countdown.", "Phiên này chưa kích hoạt đếm ngược đóng sớm.");
        put(values, "This auction is already in an early-close countdown process.", "Phiên này đã ở trong quá trình đếm ngược đóng sớm.");
        put(values, "This item already has bids and can no longer be edited.", "Mục này đã có lượt đấu giá và không thể chỉnh sửa nữa.");
        put(values, "This item can only be edited before it starts or while it is in OPEN status.", "Mục này chỉ có thể chỉnh sửa trước khi bắt đầu hoặc khi đang ở trạng thái OPEN.");
        put(values, "User not found.", "Không tìm thấy người dùng.");
        put(values, "Username already exists.", "Tên đăng nhập đã tồn tại.");
        put(values, "You cannot lock your own account.", "Bạn không thể khóa chính tài khoản của mình.");
        put(values, "You do not have permission to close this auction.", "Bạn không có quyền đóng phiên này.");
        put(values, "Your account has been locked.", "Tài khoản của bạn đã bị khóa.");
    }

    private static void addDatabaseTranslations(Map<String, String> values) {
        put(values, "PostgreSQL JDBC driver not found. Make sure the PostgreSQL driver is available.", "Không tìm thấy driver JDBC PostgreSQL. Hãy bảo đảm driver PostgreSQL đã sẵn sàng.");
        put(values, "The PostgreSQL connection pool is exhausted. Please try again.", "Pool kết nối PostgreSQL đã hết. Vui lòng thử lại.");
        put(values, "Interrupted while waiting for a PostgreSQL connection.", "Đã bị ngắt khi đang chờ kết nối PostgreSQL.");
        put(values, "PostgreSQL connection is closed.", "Kết nối PostgreSQL đã đóng.");
        put(values, "Unable to initialize the PostgreSQL connection.", "Không thể khởi tạo kết nối PostgreSQL.");
        put(values, "Unable to read the database configuration file.", "Không thể đọc tệp cấu hình cơ sở dữ liệu.");
        put(values, "Unable to save the user to PostgreSQL.", "Không thể lưu người dùng vào PostgreSQL.");
        put(values, "Unable to update the user in PostgreSQL.", "Không thể cập nhật người dùng trong PostgreSQL.");
        put(values, "Unable to find the user by username in PostgreSQL.", "Không thể tìm người dùng theo tên đăng nhập trong PostgreSQL.");
        put(values, "Unable to find the user by email in PostgreSQL.", "Không thể tìm người dùng theo email trong PostgreSQL.");
        put(values, "Unable to read the user list from PostgreSQL.", "Không thể đọc danh sách người dùng từ PostgreSQL.");
        put(values, "Unable to save the auction to PostgreSQL.", "Không thể lưu phiên đấu giá vào PostgreSQL.");
        put(values, "Unable to update the auction in PostgreSQL.", "Không thể cập nhật phiên đấu giá trong PostgreSQL.");
        put(values, "Unable to delete the auction in PostgreSQL.", "Không thể xóa phiên đấu giá trong PostgreSQL.");
        put(values, "Unable to find the auction in PostgreSQL.", "Không thể tìm phiên đấu giá trong PostgreSQL.");
        put(values, "Unable to read the auction list from PostgreSQL.", "Không thể đọc danh sách phiên đấu giá từ PostgreSQL.");
        put(values, "Unable to save the bid transaction to PostgreSQL.", "Không thể lưu giao dịch đấu giá vào PostgreSQL.");
        put(values, "Unable to read bid history from PostgreSQL.", "Không thể đọc lịch sử đấu giá từ PostgreSQL.");
        put(values, "Unable to read all bid transactions from PostgreSQL.", "Không thể đọc toàn bộ giao dịch đấu giá từ PostgreSQL.");
        put(values, "Unable to save homepage content to PostgreSQL.", "Không thể lưu nội dung trang chủ vào PostgreSQL.");
        put(values, "Unable to update homepage content in PostgreSQL.", "Không thể cập nhật nội dung trang chủ trong PostgreSQL.");
        put(values, "Unable to delete homepage content in PostgreSQL.", "Không thể xóa nội dung trang chủ trong PostgreSQL.");
        put(values, "Unable to find the homepage announcement in PostgreSQL.", "Không thể tìm thông báo trang chủ trong PostgreSQL.");
        put(values, "Unable to read homepage announcements from PostgreSQL.", "Không thể đọc thông báo trang chủ từ PostgreSQL.");
    }

    private static Map<String, String> createReverseTranslations(Map<String, String> source) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            values.put(entry.getValue(), entry.getKey());
        }
        return values;
    }

    private static void put(Map<String, String> values, String source, String target) {
        values.put(source, target);
    }
}
