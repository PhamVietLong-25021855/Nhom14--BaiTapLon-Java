package uet.auctionsystem.gui.fxml;

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
import uet.auctionsystem.model.AuctionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Ghi chu file: File controller JavaFX; dieu khien hanh vi cua man hinh, hop thoai hoac thanh phan UI.
// Khai bao lop UiText; dieu khien mot man hinh hoac thanh phan JavaFX cu the.
final class UiText {
    private static final Map<String, String> EN_TO_VI = createTranslations();
    private static final Map<String, String> VI_TO_EN = createReverseTranslations(EN_TO_VI);
    private static final Map<String, String> EN_PREFIX_TO_VI = createPrefixTranslations();
    private static final Map<String, String> VI_PREFIX_TO_EN = createReverseTranslations(EN_PREFIX_TO_VI);
    private static final List<Map.Entry<String, String>> EN_PREFIXES = sortByKeyLengthDesc(EN_PREFIX_TO_VI);
    private static final List<Map.Entry<String, String>> VI_PREFIXES = sortByKeyLengthDesc(VI_PREFIX_TO_EN);
    // Thuoc tinh: luu trang thai hoac du lieu tam cho current language.
    private static AppLanguage currentLanguage = AppLanguage.ENGLISH;
    // Ham tao: khoi tao doi tuong UiText voi cac phu thuoc can thiet.
    private UiText() {
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac set current language.
    static void setCurrentLanguage(AppLanguage language) {
        currentLanguage = language == null ? AppLanguage.ENGLISH : language;
    }
    // Phuong thuc: thuc hien chuc nang text trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang auction status trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang user status trong lop UiText.
    static String userStatus(String status) {
        return text(status);
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply.
    static void apply(Node root) {
        if (root == null) {
            return;
        }
        applyNode(root);
    }
    // Phuong thuc: thuc hien chuc nang configure translated combo box trong lop UiText.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac refresh translated combo box.
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
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply node.
    private static void applyNode(Node node) {
        if (node instanceof Labeled labeled) {
            labeled.setText(text(labeled.getText()));
        }
        if (node instanceof TextInputControl input) {
            input.setPromptText(text(input.getPromptText()));
        }
        if (node instanceof ComboBoxBase<?> comboBoxBase) {
            comboBoxBase.setPromptText(text(comboBoxBase.getPromptText()));
        }
        if (node instanceof ComboBox<?> comboBox && comboBox.getValue() instanceof String) {
            @SuppressWarnings("unchecked")
            ComboBox<String> translatedCombo = (ComboBox<String>) comboBox;
            refreshTranslatedComboBox(translatedCombo);
        }
        if (node instanceof Axis<?> axis) {
            axis.setLabel(text(axis.getLabel()));
        }
        if (node instanceof MenuButton menuButton) {
            menuButton.setText(text(menuButton.getText()));
            for (MenuItem item : menuButton.getItems()) {
                applyMenuItem(item);
            }
        }
        if (node instanceof TableView<?> tableView) {
            for (TableColumn<?, ?> column : tableView.getColumns()) {
                applyTableColumn(column);
            }
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyNode(child);
            }
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply table column.
    private static void applyTableColumn(TableColumn<?, ?> column) {
        if (column == null) {
            return;
        }
        column.setText(text(column.getText()));
        for (TableColumn<?, ?> child : column.getColumns()) {
            applyTableColumn(child);
        }
    }
    // Phuong thuc: cap nhat du lieu hoac trang thai cho thao tac apply menu item.
    private static void applyMenuItem(MenuItem item) {
        if (item == null) {
            return;
        }
        item.setText(text(item.getText()));
        if (item instanceof Menu menu) {
            for (MenuItem child : menu.getItems()) {
                applyMenuItem(child);
            }
        }
    }
    // Phuong thuc: thuc hien chuc nang translate structured trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang translate pipe segments trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang translate segment trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang translate numeric suffix trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang translate known prefix trong lop UiText.
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
    // Phuong thuc: thuc hien chuc nang count leading spaces trong lop UiText.
    private static int countLeadingSpaces(String value) {
        int count = 0;
        while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
            count++;
        }
        return count;
    }
    // Phuong thuc: thuc hien chuc nang count trailing spaces trong lop UiText.
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
        put(values, "Role: ", "Vai trÃ²: ");
        put(values, "Account: ", "TÃ i khoáº£n: ");
        put(values, "Status: ", "Tráº¡ng thÃ¡i: ");
        put(values, "Category: ", "Danh má»¥c: ");
        put(values, "Schedule: ", "Lá»‹ch: ");
        put(values, "Posted schedule: ", "Lá»‹ch Ä‘Äƒng: ");
        put(values, "Updated at: ", "Cáº­p nháº­t lÃºc: ");
        put(values, "Linked auction: ", "PhiÃªn liÃªn káº¿t: ");
        put(values, "Linked item: ", "Má»¥c liÃªn káº¿t: ");
        put(values, "Product: ", "Sáº£n pháº©m: ");
        put(values, "Remaining: ", "CÃ²n láº¡i: ");
        put(values, "Total transactions: ", "Tá»•ng giao dá»‹ch: ");
        put(values, "User ID: ", "ID ngÆ°á»i dÃ¹ng: ");
        put(values, "Price: ", "GiÃ¡: ");
        put(values, "At: ", "LÃºc: ");
        put(values, "Auction schedule update: ", "Cáº­p nháº­t lá»‹ch phiÃªn: ");
        put(values, "Auction #", "PhiÃªn #");
        put(values, "Bidder #", "NgÆ°á»i Ä‘áº¥u giÃ¡ #");
        put(values, "The amount must be higher than the current price (", "Má»©c giÃ¡ pháº£i cao hÆ¡n giÃ¡ hiá»‡n táº¡i (");
        put(values, "The amount must be higher than the starting price (", "Má»©c giÃ¡ pháº£i cao hÆ¡n giÃ¡ khá»Ÿi Ä‘iá»ƒm (");
        put(values, "AuctionScheduler error: ", "Lá»—i bá»™ láº­p lá»‹ch Ä‘áº¥u giÃ¡: ");
        put(values, "Unable to load FXML ", "KhÃ´ng thá»ƒ táº£i FXML ");
        put(values, "Database configuration file not found: ", "KhÃ´ng tÃ¬m tháº¥y tá»‡p cáº¥u hÃ¬nh cÆ¡ sá»Ÿ dá»¯ liá»‡u: ");
        put(values, "Invalid numeric value for ", "GiÃ¡ trá»‹ sá»‘ khÃ´ng há»£p lá»‡ cho ");
        return values;
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add common translations.
    private static void addCommonTranslations(Map<String, String> values) {
        put(values, "SETTINGS", "CÃ€I Äáº¶T");
        put(values, "English", "Tiáº¿ng Anh");
        put(values, "Vietnamese", "Tiáº¿ng Viá»‡t");
        put(values, "Notification", "ThÃ´ng bÃ¡o");
        put(values, "NOTIFICATION", "THÃ”NG BÃO");
        put(values, "Error", "Lá»—i");
        put(values, "ERROR", "Lá»–I");
        put(values, "Success", "ThÃ nh cÃ´ng");
        put(values, "SUCCESS", "THÃ€NH CÃ”NG");
        put(values, "Confirm", "XÃ¡c nháº­n");
        put(values, "CONFIRM", "XÃC NHáº¬N");
        put(values, "Cancel", "Há»§y");
        put(values, "CANCEL", "Há»¦Y");
        put(values, "Close", "ÄÃ³ng");
        put(values, "CLOSE", "ÄÃ“NG");
        put(values, "Input", "Nháº­p");
        put(values, "INPUT", "NHáº¬P");
        put(values, "Language updated.", "ÄÃ£ cáº­p nháº­t ngÃ´n ngá»¯.");
        put(values, "Language settings are unavailable.", "TÃ¹y chá»n ngÃ´n ngá»¯ hiá»‡n chÆ°a kháº£ dá»¥ng.");
        put(values, "LOG OUT", "ÄÄ‚NG XUáº¤T");
        put(values, "SEARCH", "TÃŒM KIáº¾M");
        put(values, "Search", "TÃ¬m kiáº¿m");
        put(values, "Sort", "Sáº¯p xáº¿p");
        put(values, "Status", "Tráº¡ng thÃ¡i");
        put(values, "Title", "TiÃªu Ä‘á»");
        put(values, "Updated", "Cáº­p nháº­t");
        put(values, "ID", "MÃƒ");
        put(values, "Username", "TÃªn Ä‘Äƒng nháº­p");
        put(values, "Email", "Email");
        put(values, "Role", "Vai trÃ²");
        put(values, "Product", "Sáº£n pháº©m");
        put(values, "Product Name", "TÃªn sáº£n pháº©m");
        put(values, "Product Image", "áº¢nh sáº£n pháº©m");
        put(values, "Category", "Danh má»¥c");
        put(values, "Description", "MÃ´ táº£");
        put(values, "Item", "Máº·t hÃ ng");
        put(values, "Current Bid", "GiÃ¡ hiá»‡n táº¡i");
        put(values, "Current Price", "GiÃ¡ hiá»‡n táº¡i");
        put(values, "CURRENT PRICE", "GIÃ HIá»†N Táº I");
        put(values, "Highest Bid", "GiÃ¡ cao nháº¥t");
        put(values, "Duration", "Thá»i lÆ°á»£ng");
        put(values, "Remaining", "CÃ²n láº¡i");
        put(values, "Schedule", "Lá»‹ch");
        put(values, "Posted schedule", "Lá»‹ch Ä‘Äƒng");
        put(values, "Updated at", "Cáº­p nháº­t lÃºc");
        put(values, "Linked auction", "PhiÃªn liÃªn káº¿t");
        put(values, "Display Schedule", "Lá»‹ch hiá»ƒn thá»‹");
        put(values, "Time Window", "Khung thá»i gian");
        put(values, "Linked Auction", "PhiÃªn liÃªn káº¿t");
        put(values, "Early Close", "ÄÃ³ng sá»›m");
        put(values, "Linked item: -", "Má»¥c liÃªn káº¿t: -");
        put(values, "Additional details", "Chi tiáº¿t bá»• sung");
        put(values, "DETAILS", "CHI TIáº¾T");
        put(values, "Announcement Title", "TiÃªu Ä‘á» thÃ´ng bÃ¡o");
        put(values, "Announcement summary", "TÃ³m táº¯t thÃ´ng bÃ¡o");
        put(values, "Not linked", "ChÆ°a liÃªn káº¿t");
        put(values, "No linked auction", "KhÃ´ng cÃ³ phiÃªn liÃªn káº¿t");
        put(values, "LIVE", "TRá»°C TIáº¾P");
        put(values, "OPENING SOON", "Sáº®P Má»ž");
        put(values, "RUNNING", "ÄANG DIá»„N RA");
        put(values, "FINISHED", "ÄÃƒ Káº¾T THÃšC");
        put(values, "CANCELLED", "ÄÃƒ Há»¦Y");
        put(values, "PAID", "ÄÃƒ THANH TOÃN");
        put(values, "ACTIVE", "HOáº T Äá»˜NG");
        put(values, "BLOCKED", "ÄÃƒ KHÃ“A");
        put(values, "ACCEPTED", "ÄÆ¯á»¢C CHáº¤P NHáº¬N");
        put(values, "Admin", "Quáº£n trá»‹ viÃªn");
        put(values, "Seller", "NgÆ°á»i bÃ¡n");
        put(values, "Bidder", "NgÆ°á»i Ä‘áº¥u giÃ¡");
        put(values, "Admin CMS", "CMS quáº£n trá»‹");
        put(values, "ADMIN", "QUáº¢N TRá»Š");
        put(values, "SELLER", "NGÆ¯á»œI BÃN");
        put(values, "BIDDER", "NGÆ¯á»œI Äáº¤U GIÃ");
        put(values, "All", "Táº¥t cáº£");
        put(values, "Running", "Äang diá»…n ra");
        put(values, "Opening Soon", "Sáº¯p má»Ÿ");
        put(values, "Finished", "ÄÃ£ káº¿t thÃºc");
        put(values, "Default", "Máº·c Ä‘á»‹nh");
        put(values, "Product Name A-Z", "TÃªn sáº£n pháº©m A-Z");
        put(values, "Highest Bid Descending", "GiÃ¡ cao nháº¥t giáº£m dáº§n");
        put(values, "Ending Soon", "Sáº¯p káº¿t thÃºc");
        put(values, "Category A-Z", "Danh má»¥c A-Z");
        put(values, "Not active", "ChÆ°a kÃ­ch hoáº¡t");
        put(values, "Not started", "ChÆ°a báº¯t Ä‘áº§u");
        put(values, "Ended", "ÄÃ£ káº¿t thÃºc");
        put(values, "counts left", "lÆ°á»£t Ä‘áº¿m cÃ²n láº¡i");
        put(values, "transactions", "giao dá»‹ch");
        put(values, "bid", "lÆ°á»£t Ä‘áº¥u giÃ¡");
        put(values, "min", "phÃºt");
        put(values, "minutes", "phÃºt");
        put(values, "sec", "giÃ¢y");
        put(values, "Auction", "PhiÃªn");
        put(values, "Auction #", "PhiÃªn #");
        put(values, "Auction schedule update", "Cáº­p nháº­t lá»‹ch phiÃªn");
        put(values, "Bidder #", "NgÆ°á»i Ä‘áº¥u giÃ¡ #");
        put(values, "Account:", "TÃ i khoáº£n:");
        put(values, "Account: -", "TÃ i khoáº£n: -");
        put(values, "Seller ID", "ID ngÆ°á»i bÃ¡n");
        put(values, "User ID", "ID ngÆ°á»i dÃ¹ng");
        put(values, "Price", "GiÃ¡");
        put(values, "At", "LÃºc");
        put(values, "Total transactions", "Tá»•ng giao dá»‹ch");
        put(values, "No bid transactions yet.", "ChÆ°a cÃ³ giao dá»‹ch Ä‘áº¥u giÃ¡ nÃ o.");
        put(values, "New bid activity will appear here immediately.", "Hoáº¡t Ä‘á»™ng ra giÃ¡ má»›i sáº½ xuáº¥t hiá»‡n ngay táº¡i Ä‘Ã¢y.");
        put(values, "This product does not have a detailed description yet.", "Sáº£n pháº©m nÃ y chÆ°a cÃ³ mÃ´ táº£ chi tiáº¿t.");
        put(values, "No leading bidder yet", "ChÆ°a cÃ³ ngÆ°á»i dáº«n Ä‘áº§u");
        put(values, "You are leading", "Báº¡n Ä‘ang dáº«n Ä‘áº§u");
        put(values, "YOU ARE LEADING", "Báº N ÄANG DáºªN Äáº¦U");
        put(values, "LEADING BIDDER", "NGÆ¯á»œI DáºªN Äáº¦U");
        put(values, "BID COUNT", "Sá» LÆ¯á»¢T Äáº¤U GIÃ");
        put(values, "COUNTDOWN", "Äáº¾M NGÆ¯á»¢C");
        put(values, "SCHEDULE", "Lá»ŠCH");
        put(values, "BID HISTORY", "Lá»ŠCH Sá»¬ Äáº¤U GIÃ");
        put(values, "NO AUCTION SELECTED", "CHÆ¯A CHá»ŒN PHIÃŠN Äáº¤U GIÃ");
        put(values, "Realtime", "Thá»i gian thá»±c");
        put(values, "REALTIME", "THá»œI GIAN THá»°C");
        put(values, "SECURE", "Báº¢O Máº¬T");
        put(values, "1s", "1 giÃ¢y");
        put(values, "Instruction text", "Ná»™i dung hÆ°á»›ng dáº«n");
        put(values, "Error message", "ThÃ´ng bÃ¡o lá»—i");
        put(values, "Notification content", "Ná»™i dung thÃ´ng bÃ¡o");
        put(values, "0 transactions", "0 giao dá»‹ch");
        put(values, "30 minutes", "30 phÃºt");
        put(values, "Enter value", "Nháº­p giÃ¡ trá»‹");
        put(values, "A short summary will appear here.", "Pháº§n tÃ³m táº¯t ngáº¯n sáº½ hiá»ƒn thá»‹ táº¡i Ä‘Ã¢y.");
        put(values, "Additional details and instructions will appear here.", "Chi tiáº¿t vÃ  hÆ°á»›ng dáº«n bá»• sung sáº½ hiá»ƒn thá»‹ táº¡i Ä‘Ã¢y.");
        put(values, "Bid status information will appear here.", "ThÃ´ng tin tráº¡ng thÃ¡i ra giÃ¡ sáº½ hiá»ƒn thá»‹ táº¡i Ä‘Ã¢y.");
        put(values, "Additional details, notes, instructions...", "Chi tiáº¿t, ghi chÃº, hÆ°á»›ng dáº«n bá»• sung...");
        put(values, "Short summary displayed on the homepage", "TÃ³m táº¯t ngáº¯n hiá»ƒn thá»‹ trÃªn trang chá»§");
        put(values, "Auction schedule information shown on the homepage", "ThÃ´ng tin lá»‹ch phiÃªn hiá»ƒn thá»‹ trÃªn trang chá»§");
        put(values, "Example: 1500000", "VÃ­ dá»¥: 1500000");
        put(values, "Example: Electronics", "VÃ­ dá»¥: Äiá»‡n tá»­");
        put(values, "A", "A");
        put(values, "AD", "QT");
        put(values, "24/7", "24/7");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add landing translations.
    private static void addLandingTranslations(Map<String, String> values) {
        put(values, "PRODUCT AUCTION PLATFORM", "Ná»€N Táº¢NG Äáº¤U GIÃ Sáº¢N PHáº¨M");
        put(values, "PREMIUM AUCTION APP", "á»¨NG Dá»¤NG Äáº¤U GIÃ CAO Cáº¤P");
        put(values, "FAST OVERVIEW", "Tá»”NG QUAN NHANH");
        put(values, "LIVE AUCTION EXPERIENCE", "TRáº¢I NGHIá»†M Äáº¤U GIÃ TRá»°C TIáº¾P");
        put(values, "AUCTION HOUSE STUDIO", "KHÃ”NG GIAN NHÃ€ Äáº¤U GIÃ");
        put(values, "AUCTION HOUSE", "NHÃ€ Äáº¤U GIÃ");
        put(values, "Online auctions for bidders, sellers, and admins", "Äáº¥u giÃ¡ trá»±c tuyáº¿n cho ngÆ°á»i Ä‘áº¥u giÃ¡, ngÆ°á»i bÃ¡n vÃ  quáº£n trá»‹ viÃªn");
        put(values, "Browse live auctions, see upcoming listings, and follow admin updates from one homepage.", "Theo dÃµi phiÃªn Ä‘ang diá»…n ra, xem lá»‹ch sáº¯p má»Ÿ vÃ  cáº­p nháº­t quáº£n trá»‹ ngay trÃªn má»™t trang chá»§.");
        put(values, "Log in to bid, sell, or manage the platform.", "ÄÄƒng nháº­p Ä‘á»ƒ Ä‘áº¥u giÃ¡, bÃ¡n hÃ ng hoáº·c quáº£n lÃ½ ná»n táº£ng.");
        put(values, "Live stats, featured auctions, and homepage updates stay in one place", "Thá»‘ng kÃª trá»±c tiáº¿p, phiÃªn ná»•i báº­t vÃ  cáº­p nháº­t trang chá»§ Ä‘Æ°á»£c gom trong má»™t nÆ¡i");
        put(values, "The homepage now opens straight to the information users actually need.", "Trang chá»§ giá» Ä‘i tháº³ng vÃ o nhá»¯ng thÃ´ng tin ngÆ°á»i dÃ¹ng thá»±c sá»± cáº§n.");
        put(values, "Counts and featured content refresh automatically without crowding the screen.", "Sá»‘ liá»‡u vÃ  ná»™i dung ná»•i báº­t tá»± lÃ m má»›i mÃ  váº«n giá»¯ giao diá»‡n gá»n gÃ ng.");
        put(values, "FEATURED AUCTIONS AND HOMEPAGE UPDATES", "PHIÃŠN Ná»”I Báº¬T VÃ€ Cáº¬P NHáº¬T TRANG CHá»¦");
        put(values, "Live and upcoming auctions are listed beside homepage announcements.", "CÃ¡c phiÃªn Ä‘ang diá»…n ra vÃ  sáº¯p má»Ÿ Ä‘Æ°á»£c hiá»ƒn thá»‹ cáº¡nh thÃ´ng bÃ¡o trang chá»§.");
        put(values, "AUCTION SPOTLIGHT", "PHIÃŠN Ná»”I Báº¬T");
        put(values, "HOMEPAGE UPDATES", "Cáº¬P NHáº¬T TRANG CHá»¦");
        put(values, "LIVE AUCTIONS", "PHIÃŠN ÄANG DIá»„N RA");
        put(values, "ADMIN UPDATES", "Cáº¬P NHáº¬T Tá»ª QUáº¢N TRá»Š");
        put(values, "No auctions available yet", "ChÆ°a cÃ³ phiÃªn Ä‘áº¥u giÃ¡ nÃ o.");
        put(values, "When a seller creates a new auction or one begins, this list updates automatically.", "Khi ngÆ°á»i bÃ¡n táº¡o phiÃªn má»›i hoáº·c phiÃªn báº¯t Ä‘áº§u, danh sÃ¡ch nÃ y sáº½ tá»± cáº­p nháº­t.");
        put(values, "No admin announcements yet", "ChÆ°a cÃ³ thÃ´ng bÃ¡o quáº£n trá»‹.");
        put(values, "Announcements, auction schedules, and featured items will appear here after admins publish updates.", "ThÃ´ng bÃ¡o, lá»‹ch phiÃªn vÃ  ná»™i dung ná»•i báº­t sáº½ xuáº¥t hiá»‡n táº¡i Ä‘Ã¢y sau khi quáº£n trá»‹ Ä‘Äƒng táº£i.");
        put(values, "Information will be updated later", "ThÃ´ng tin sáº½ Ä‘Æ°á»£c cáº­p nháº­t sau.");
        put(values, "Content will be updated later", "Ná»™i dung sáº½ Ä‘Æ°á»£c cáº­p nháº­t sau.");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add dashboard translations.
    private static void addDashboardTranslations(Map<String, String> values) {
        put(values, "Seller Dashboard", "Báº£ng Ä‘iá»u khiá»ƒn ngÆ°á»i bÃ¡n");
        put(values, "Bidder Dashboard", "Báº£ng Ä‘iá»u khiá»ƒn ngÆ°á»i Ä‘áº¥u giÃ¡");
        put(values, "Admin Dashboard", "Báº£ng Ä‘iá»u khiá»ƒn quáº£n trá»‹");
        put(values, "Homepage Content Manager", "Quáº£n lÃ½ ná»™i dung trang chá»§");
        put(values, "Create, edit, and monitor auctions from one workspace.", "Táº¡o, chá»‰nh sá»­a vÃ  theo dÃµi phiÃªn Ä‘áº¥u giÃ¡ trong má»™t khu lÃ m viá»‡c.");
        put(values, "Create, edit, and monitor auctions from one screen.", "Táº¡o, chá»‰nh sá»­a vÃ  theo dÃµi phiÃªn Ä‘áº¥u giÃ¡ trÃªn má»™t mÃ n hÃ¬nh.");
        put(values, "Use the form on the left and the live preview on the right.", "DÃ¹ng biá»ƒu máº«u bÃªn trÃ¡i vÃ  xem trÆ°á»›c trá»±c tiáº¿p bÃªn pháº£i.");
        put(values, "Bidder Workspace", "KhÃ´ng gian ngÆ°á»i Ä‘áº¥u giÃ¡");
        put(values, "Track auctions, review live details, and bid without switching screens.", "Theo dÃµi phiÃªn, xem chi tiáº¿t trá»±c tiáº¿p vÃ  Ä‘áº·t giÃ¡ mÃ  khÃ´ng cáº§n Ä‘á»•i mÃ n hÃ¬nh.");
        put(values, "Search auctions, inspect details, and place bids quickly.", "TÃ¬m kiáº¿m phiÃªn, xem chi tiáº¿t vÃ  Ä‘áº·t giÃ¡ nhanh chÃ³ng.");
        put(values, "Monitor users, auctions, and system activity from one dashboard.", "Theo dÃµi ngÆ°á»i dÃ¹ng, phiÃªn Ä‘áº¥u giÃ¡ vÃ  hoáº¡t Ä‘á»™ng há»‡ thá»‘ng trÃªn má»™t báº£ng Ä‘iá»u khiá»ƒn.");
        put(values, "Create and schedule homepage announcements from one screen.", "Táº¡o vÃ  lÃªn lá»‹ch thÃ´ng bÃ¡o trang chá»§ trÃªn má»™t mÃ n hÃ¬nh.");
        put(values, "Create and schedule homepage announcements.", "Táº¡o vÃ  lÃªn lá»‹ch thÃ´ng bÃ¡o trang chá»§.");
        put(values, "Edit content, preview it, and manage published announcements in one place.", "Chá»‰nh sá»­a ná»™i dung, xem trÆ°á»›c vÃ  quáº£n lÃ½ thÃ´ng bÃ¡o Ä‘Ã£ Ä‘Äƒng trong cÃ¹ng má»™t nÆ¡i.");
        put(values, "Monitor users, auctions, and homepage activity from one control panel.", "Theo dÃµi ngÆ°á»i dÃ¹ng, phiÃªn Ä‘áº¥u giÃ¡ vÃ  hoáº¡t Ä‘á»™ng trang chá»§ trÃªn má»™t báº£ng Ä‘iá»u khiá»ƒn.");
        put(values, "Use the tables below for account actions, homepage access, and auction control.", "DÃ¹ng cÃ¡c báº£ng bÃªn dÆ°á»›i Ä‘á»ƒ thao tÃ¡c tÃ i khoáº£n, vÃ o trang chá»§ vÃ  Ä‘iá»u khiá»ƒn phiÃªn Ä‘áº¥u giÃ¡.");

        put(values, "MODULES", "MÃ”-ÄUN");
        put(values, "USER SESSION", "PHIÃŠN NGÆ¯á»œI DÃ™NG");
        put(values, "Auction Studio", "XÆ°á»Ÿng phiÃªn Ä‘áº¥u giÃ¡");
        put(values, "My Auctions", "PhiÃªn cá»§a tÃ´i");
        put(values, "Bid History", "Lá»‹ch sá»­ Ä‘áº¥u giÃ¡");
        put(values, "Overview", "Tá»•ng quan");
        put(values, "Users", "NgÆ°á»i dÃ¹ng");
        put(values, "Auctions", "PhiÃªn Ä‘áº¥u giÃ¡");
        put(values, "Homepage", "Trang chá»§");
        put(values, "Announcements", "ThÃ´ng bÃ¡o");
        put(values, "Homepage Auctions", "PhiÃªn cho trang chá»§");
        put(values, "Homepage CMS", "CMS trang chá»§");
        put(values, "SELLER STUDIO", "KHU NGÆ¯á»œI BÃN");
        put(values, "LIVE BIDDING FLOOR", "SÃ€N Äáº¤U GIÃ TRá»°C TIáº¾P");
        put(values, "HOMEPAGE CMS", "CMS TRANG CHá»¦");
        put(values, "SYSTEM CONTROL CENTER", "TRUNG TÃ‚M ÄIá»€U KHIá»‚N Há»† THá»NG");

        put(values, "TOTAL AUCTIONS", "Tá»”NG Sá» PHIÃŠN");
        put(values, "OPEN / OPENING SOON", "ÄANG Má»ž / Sáº®P Má»ž");
        put(values, "RUNNING AUCTIONS", "PHIÃŠN ÄANG DIá»„N RA");
        put(values, "ENDING SOON", "Sáº®P Káº¾T THÃšC");
        put(values, "TOTAL USERS", "Tá»”NG NGÆ¯á»œI DÃ™NG");
        put(values, "TOTAL BIDS", "Tá»”NG LÆ¯á»¢T Äáº¤U GIÃ");
        put(values, "TOTAL ANNOUNCEMENTS", "Tá»”NG THÃ”NG BÃO");
        put(values, "LINKED AUCTIONS", "PHIÃŠN LIÃŠN Káº¾T");
        put(values, "VISIBLE AUCTIONS", "PHIÃŠN HIá»‚N THá»Š");

        put(values, "CREATE / EDIT AUCTION STUDIO", "Táº O / CHá»ˆNH Sá»¬A PHIÃŠN Äáº¤U GIÃ");
        put(values, "MY AUCTIONS", "PHIÃŠN Cá»¦A TÃ”I");
        put(values, "AUCTION LIST", "DANH SÃCH PHIÃŠN Äáº¤U GIÃ");
        put(values, "PRICE OVER TIME", "BIáº¾N Äá»˜NG GIÃ THEO THá»œI GIAN");
        put(values, "QUICK BID", "RA GIÃ NHANH");
        put(values, "LIVE BID FEED", "DÃ’NG RA GIÃ TRá»°C TIáº¾P");
        put(values, "AUCTION DETAIL", "CHI TIáº¾T PHIÃŠN");
        put(values, "LIVE PREVIEW", "XEM TRÆ¯á»šC TRá»°C TIáº¾P");
        put(values, "CREATE / EDIT HOMEPAGE ANNOUNCEMENT", "Táº O / CHá»ˆNH Sá»¬A THÃ”NG BÃO TRANG CHá»¦");
        put(values, "CURRENT ANNOUNCEMENTS", "THÃ”NG BÃO HIá»†N Táº I");
        put(values, "AUCTIONS READY FOR HOMEPAGE", "PHIÃŠN Sáº´N SÃ€NG CHO TRANG CHá»¦");
        put(values, "NAVIGATION", "ÄIá»€U HÆ¯á»šNG");
        put(values, "AUCTION OPERATIONS", "THAO TÃC PHIÃŠN Äáº¤U GIÃ");
        put(values, "AUCTION OVERVIEW", "Tá»”NG QUAN PHIÃŠN");
        put(values, "QUICK INFO", "THÃ”NG TIN NHANH");
        put(values, "USER LIST", "DANH SÃCH NGÆ¯á»œI DÃ™NG");
        put(values, "CHANGE PASSWORD", "Äá»”I Máº¬T KHáº¨U");

        put(values, "EDIT SELECTED", "Sá»¬A Má»¤C ÄÃƒ CHá»ŒN");
        put(values, "DELETE / CANCEL AUCTION", "XÃ“A / Há»¦Y PHIÃŠN");
        put(values, "CLOSE EARLY", "ÄÃ“NG Sá»šM");
        put(values, "PLACE BID NOW", "Äáº¶T GIÃ NGAY");
        put(values, "REFRESH USERS", "LÃ€M Má»šI NGÆ¯á»œI DÃ™NG");
        put(values, "REFRESH AUCTIONS", "LÃ€M Má»šI PHIÃŠN");
        put(values, "LOCK / UNLOCK", "KHÃ“A / Má»ž KHÃ“A");
        put(values, "START 3-COUNTDOWN", "Báº®T Äáº¦U Äáº¾M 3");
        put(values, "CANCEL COUNTDOWN", "Há»¦Y Äáº¾M NGÆ¯á»¢C");
        put(values, "MANAGE HOMEPAGE", "QUáº¢N LÃ TRANG CHá»¦");
        put(values, "EDIT ANNOUNCEMENT", "Sá»¬A THÃ”NG BÃO");
        put(values, "DELETE ANNOUNCEMENT", "XÃ“A THÃ”NG BÃO");
        put(values, "BACK TO ADMIN DASHBOARD", "QUAY Láº I Báº¢NG ÄIá»€U KHIá»‚N");
        put(values, "USE SELECTED AUCTION SCHEDULE", "DÃ™NG Lá»ŠCH Cá»¦A PHIÃŠN ÄÃƒ CHá»ŒN");
        put(values, "CLEAR FORM", "XÃ“A BIá»‚U MáºªU");
        put(values, "PUBLISH TO HOMEPAGE", "ÄÄ‚NG LÃŠN TRANG CHá»¦");
        put(values, "REFRESH", "LÃ€M Má»šI");
        put(values, "CHOOSE IMAGE", "CHá»ŒN áº¢NH");
        put(values, "CREATE NEW", "Táº O Má»šI");
        put(values, "CANCEL UPDATE", "Há»¦Y Cáº¬P NHáº¬T");
        put(values, "Auction Duration (minutes)", "Thá»i lÆ°á»£ng phiÃªn (phÃºt)");
        put(values, "Starting Price", "GiÃ¡ khá»Ÿi Ä‘iá»ƒm");
        put(values, "STARTING PRICE", "GIÃ KHá»žI ÄIá»‚M");
        put(values, "Enter product name", "Nháº­p tÃªn sáº£n pháº©m");
        put(values, "Detailed description of the product so bidders understand it better before placing a bid", "MÃ´ táº£ chi tiáº¿t sáº£n pháº©m Ä‘á»ƒ ngÆ°á»i Ä‘áº¥u giÃ¡ hiá»ƒu rÃµ hÆ¡n trÆ°á»›c khi ra giÃ¡");
        put(values, "Local file path or product image URL", "ÄÆ°á»ng dáº«n áº£nh cá»¥c bá»™ hoáº·c URL áº£nh sáº£n pháº©m");
        put(values, "Use .png, .jpg, or .jpeg for clearer previews.", "DÃ¹ng .png, .jpg hoáº·c .jpeg Ä‘á»ƒ xem trÆ°á»›c rÃµ hÆ¡n.");
        put(values, "Choose product image", "Chá»n áº£nh sáº£n pháº©m");
        put(values, "Image Files", "Tá»‡p hÃ¬nh áº£nh");
        put(values, "The description updates instantly as the seller types.", "MÃ´ táº£ sáº½ cáº­p nháº­t ngay khi ngÆ°á»i bÃ¡n nháº­p.");
        put(values, "Creating a new auction", "Äang táº¡o phiÃªn má»›i");
        put(values, "Editing auction", "Äang chá»‰nh sá»­a phiÃªn");
        put(values, "Invalid price", "GiÃ¡ khÃ´ng há»£p lá»‡");
        put(values, "Select an auction to view details", "Chá»n má»™t phiÃªn Ä‘á»ƒ xem chi tiáº¿t");
        put(values, "Select an auction to view details.", "Chá»n má»™t phiÃªn Ä‘á»ƒ xem chi tiáº¿t.");
        put(values, "The product description will appear here.", "MÃ´ táº£ sáº£n pháº©m sáº½ hiá»ƒn thá»‹ táº¡i Ä‘Ã¢y.");
        put(values, "Search by product name or category", "TÃ¬m theo tÃªn sáº£n pháº©m hoáº·c danh má»¥c");
        put(values, "STATUS FILTER", "Bá»˜ Lá»ŒC TRáº NG THÃI");
        put(values, "STATUS FORM", "TRáº NG THÃI BIá»‚U MáºªU");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add form translations.
    private static void addFormTranslations(Map<String, String> values) {
        put(values, "LOG IN", "ÄÄ‚NG NHáº¬P");
        put(values, "REGISTER", "ÄÄ‚NG KÃ");
        put(values, "CREATE NEW ACCOUNT", "Táº O TÃ€I KHOáº¢N Má»šI");
        put(values, "ACCOUNT LOGIN", "ÄÄ‚NG NHáº¬P TÃ€I KHOáº¢N");
        put(values, "ACCOUNT REGISTRATION", "ÄÄ‚NG KÃ TÃ€I KHOáº¢N");
        put(values, "BACK TO LOGIN", "QUAY Láº I ÄÄ‚NG NHáº¬P");
        put(values, "COMPLETE REGISTRATION", "HOÃ€N Táº¤T ÄÄ‚NG KÃ");
        put(values, "Password", "Máº­t kháº©u");
        put(values, "Full Name", "Há» vÃ  tÃªn");
        put(values, "Confirm Password", "XÃ¡c nháº­n máº­t kháº©u");
        put(values, "Enter your username", "Nháº­p tÃªn Ä‘Äƒng nháº­p");
        put(values, "Enter your password", "Nháº­p máº­t kháº©u");
        put(values, "Enter username", "Nháº­p tÃªn Ä‘Äƒng nháº­p");
        put(values, "Enter your full name", "Nháº­p há» vÃ  tÃªn");
        put(values, "Enter email address", "Nháº­p Ä‘á»‹a chá»‰ email");
        put(values, "Confirm your password", "Nháº­p láº¡i máº­t kháº©u");
        put(values, "Remember this login session", "Ghi nhá»› phiÃªn Ä‘Äƒng nháº­p nÃ y");
        put(values, "Show password", "Hiá»ƒn thá»‹ máº­t kháº©u");
        put(values, "Forgot password?", "QuÃªn máº­t kháº©u?");
        put(values, "Current Password", "Máº­t kháº©u hiá»‡n táº¡i");
        put(values, "New Password", "Máº­t kháº©u má»›i");
        put(values, "Enter current password", "Nháº­p máº­t kháº©u hiá»‡n táº¡i");
        put(values, "Enter new password", "Nháº­p máº­t kháº©u má»›i");
        put(values, "Use your existing username and password to enter the auction workspace.", "DÃ¹ng tÃ i khoáº£n hiá»‡n cÃ³ Ä‘á»ƒ vÃ o há»‡ thá»‘ng Ä‘áº¥u giÃ¡.");
        put(values, "Complete all required information to join the online auction system.", "HoÃ n táº¥t Ä‘áº§y Ä‘á»§ thÃ´ng tin Ä‘á»ƒ tham gia há»‡ thá»‘ng Ä‘áº¥u giÃ¡ trá»±c tuyáº¿n.");
        put(values, "Create a new auction account", "Táº¡o tÃ i khoáº£n Ä‘áº¥u giÃ¡ má»›i");
        put(values, "Register once, then sign in as a bidder, seller, or admin.", "ÄÄƒng kÃ½ má»™t láº§n rá»“i Ä‘Äƒng nháº­p vá»›i vai trÃ² ngÆ°á»i Ä‘áº¥u giÃ¡, ngÆ°á»i bÃ¡n hoáº·c quáº£n trá»‹.");
        put(values, "Register once, then sign in as a bidder or seller.", "ÄÄƒng kÃ½ má»™t láº§n rá»“i Ä‘Äƒng nháº­p vá»›i vai trÃ² ngÆ°á»i Ä‘áº¥u giÃ¡ hoáº·c ngÆ°á»i bÃ¡n.");
        put(values, "Sign in to the auction platform", "ÄÄƒng nháº­p vÃ o ná»n táº£ng Ä‘áº¥u giÃ¡");
        put(values, "Access bidder, seller, or admin workspaces from one account system.", "Truy cáº­p khu ngÆ°á»i Ä‘áº¥u giÃ¡, ngÆ°á»i bÃ¡n hoáº·c quáº£n trá»‹ tá»« má»™t há»‡ thá»‘ng tÃ i khoáº£n.");
        put(values, "Live Validation", "Kiá»ƒm tra trá»±c tiáº¿p");
        put(values, "Email, password, and password confirmation are validated while you type.", "Email, máº­t kháº©u vÃ  xÃ¡c nháº­n máº­t kháº©u Ä‘Æ°á»£c kiá»ƒm tra ngay khi báº¡n nháº­p.");
        put(values, "A valid password must have at least 6 characters and include both letters and numbers.", "Máº­t kháº©u há»£p lá»‡ pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘.");
        put(values, "A valid password must be at least 6 characters long and include letters and numbers.", "Máº­t kháº©u há»£p lá»‡ pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘.");
        put(values, "Password must be at least 6 characters and include letters and numbers", "Máº­t kháº©u pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘");
        put(values, "Password must be at least 6 characters long and include letters and numbers.", "Máº­t kháº©u pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘.");
        put(values, "Password looks good. You can continue registration.", "Máº­t kháº©u há»£p lá»‡. Báº¡n cÃ³ thá»ƒ tiáº¿p tá»¥c Ä‘Äƒng kÃ½.");
        put(values, "Password must be at least 6 characters long with letters and numbers. Confirmation must match.", "Máº­t kháº©u pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»±, gá»“m chá»¯ vÃ  sá»‘, Ä‘á»“ng thá»i pháº§n xÃ¡c nháº­n pháº£i khá»›p.");
        put(values, "Please review the registration information.", "Vui lÃ²ng kiá»ƒm tra láº¡i thÃ´ng tin Ä‘Äƒng kÃ½.");
        put(values, "Invalid email.", "Email khÃ´ng há»£p lá»‡.");
        put(values, "Password confirmation does not match.", "XÃ¡c nháº­n máº­t kháº©u khÃ´ng khá»›p.");
        put(values, "Invalid role.", "Vai trÃ² khÃ´ng há»£p lá»‡.");
        put(values, "Please fill in all required information.", "Vui lÃ²ng Ä‘iá»n Ä‘áº§y Ä‘á»§ thÃ´ng tin báº¯t buá»™c.");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add runtime translations.
    private static void addRuntimeTranslations(Map<String, String> values) {
        put(values, "SAVE CHANGES", "LÆ¯U THAY Äá»”I");
        put(values, "UPDATE", "Cáº¬P NHáº¬T");
        put(values, "Login failed", "ÄÄƒng nháº­p tháº¥t báº¡i");
        put(values, "LOGIN FAILED", "ÄÄ‚NG NHáº¬P THáº¤T Báº I");
        put(values, "Login failed.", "ÄÄƒng nháº­p tháº¥t báº¡i.");
        put(values, "AuthController has not been assigned to RegisterViewController.", "AuthController chÆ°a Ä‘Æ°á»£c gÃ¡n cho RegisterViewController.");
        put(values, "Admin accounts cannot be created from registration.", "KhÃ´ng thá»ƒ táº¡o tÃ i khoáº£n quáº£n trá»‹ tá»« mÃ n hÃ¬nh Ä‘Äƒng kÃ½.");
        put(values, "Registration completed successfully. Please log in.", "ÄÄƒng kÃ½ thÃ nh cÃ´ng. Vui lÃ²ng Ä‘Äƒng nháº­p.");
        put(values, "Unable to complete registration.", "KhÃ´ng thá»ƒ hoÃ n táº¥t Ä‘Äƒng kÃ½ lÃºc nÃ y.");
        put(values, "AuthController has not been assigned to LoginViewController.", "AuthController chÆ°a Ä‘Æ°á»£c gÃ¡n cho LoginViewController.");
        put(values, "Please enter both username and password.", "Vui lÃ²ng nháº­p cáº£ tÃªn Ä‘Äƒng nháº­p vÃ  máº­t kháº©u.");
        put(values, "A dedicated password recovery flow is not available in this version.", "PhiÃªn báº£n nÃ y chÆ°a cÃ³ luá»“ng khÃ´i phá»¥c máº­t kháº©u riÃªng.");
        put(values, "Please contact an admin for password assistance.", "Vui lÃ²ng liÃªn há»‡ quáº£n trá»‹ viÃªn Ä‘á»ƒ Ä‘Æ°á»£c há»— trá»£ máº­t kháº©u.");
        put(values, "Not enough information to change the password.", "KhÃ´ng Ä‘á»§ thÃ´ng tin Ä‘á»ƒ Ä‘á»•i máº­t kháº©u.");
        put(values, "Please enter both the current password and the new password.", "Vui lÃ²ng nháº­p cáº£ máº­t kháº©u hiá»‡n táº¡i vÃ  máº­t kháº©u má»›i.");
        put(values, "Password changed successfully.", "Äá»•i máº­t kháº©u thÃ nh cÃ´ng.");
        put(values, "Unable to change the password right now.", "Hiá»‡n khÃ´ng thá»ƒ Ä‘á»•i máº­t kháº©u.");
        put(values, "Unable to load admin data.", "KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u quáº£n trá»‹.");
        put(values, "Unable to load auction data.", "KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u phiÃªn Ä‘áº¥u giÃ¡.");
        put(values, "Unable to load homepage data", "KhÃ´ng thá»ƒ táº£i dá»¯ liá»‡u trang chá»§");
        put(values, "This screen has not been connected to AuthFrame yet.", "MÃ n hÃ¬nh nÃ y chÆ°a Ä‘Æ°á»£c káº¿t ná»‘i vá»›i AuthFrame.");
        put(values, "Current admin information is unavailable.", "ThÃ´ng tin quáº£n trá»‹ hiá»‡n khÃ´ng kháº£ dá»¥ng.");
        put(values, "HomepageController has not been assigned to the admin screen.", "HomepageController chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh quáº£n trá»‹.");
        put(values, "Please select a running auction.", "Vui lÃ²ng chá»n má»™t phiÃªn Ä‘ang diá»…n ra.");
        put(values, "The 3-count early close countdown has started. If no new bid arrives, the auction will close early.", "ÄÃ£ báº¯t Ä‘áº§u Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m 3 nhá»‹p. Náº¿u khÃ´ng cÃ³ giÃ¡ má»›i, phiÃªn sáº½ Ä‘Ã³ng sá»›m.");
        put(values, "Please select an auction with an active early-close countdown.", "Vui lÃ²ng chá»n má»™t phiÃªn Ä‘ang cÃ³ Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m.");
        put(values, "The early-close countdown has been cancelled.", "ÄÃ£ há»§y Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m.");
        put(values, "AuthController has not been assigned to the admin screen.", "AuthController chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh quáº£n trá»‹.");
        put(values, "Current user information is unavailable.", "ThÃ´ng tin ngÆ°á»i dÃ¹ng hiá»‡n khÃ´ng kháº£ dá»¥ng.");
        put(values, "Please select an account.", "Vui lÃ²ng chá»n má»™t tÃ i khoáº£n.");
        put(values, "Account status updated successfully.", "ÄÃ£ cáº­p nháº­t tráº¡ng thÃ¡i tÃ i khoáº£n thÃ nh cÃ´ng.");
        put(values, "Current user is unavailable.", "NgÆ°á»i dÃ¹ng hiá»‡n khÃ´ng kháº£ dá»¥ng.");
        put(values, "The change-password action is prepared. Connect this controller to AuthFrame when integrating.", "Chá»©c nÄƒng Ä‘á»•i máº­t kháº©u Ä‘Ã£ sáºµn sÃ ng. HÃ£y káº¿t ná»‘i controller nÃ y vá»›i AuthFrame khi tÃ­ch há»£p.");
        put(values, "The logout action is prepared. Connect this controller to AuthFrame when integrating.", "Chá»©c nÄƒng Ä‘Äƒng xuáº¥t Ä‘Ã£ sáºµn sÃ ng. HÃ£y káº¿t ná»‘i controller nÃ y vá»›i AuthFrame khi tÃ­ch há»£p.");
        put(values, "Select a user to view a summary here.", "Chá»n má»™t ngÆ°á»i dÃ¹ng Ä‘á»ƒ xem tÃ³m táº¯t táº¡i Ä‘Ã¢y.");
        put(values, "Select an auction to track its countdown and status here.", "Chá»n má»™t phiÃªn Ä‘á»ƒ theo dÃµi Ä‘áº¿m ngÆ°á»£c vÃ  tráº¡ng thÃ¡i táº¡i Ä‘Ã¢y.");
        put(values, "AuctionController has not been assigned to the admin screen.", "AuctionController chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh quáº£n trá»‹.");
        put(values, "Unable to complete this action right now.", "Hiá»‡n khÃ´ng thá»ƒ hoÃ n táº¥t thao tÃ¡c nÃ y.");
        put(values, "Required controllers have not been assigned to the homepage management screen.", "CÃ¡c controller cáº§n thiáº¿t chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh quáº£n lÃ½ trang chá»§.");
        put(values, "Please select an announcement to edit.", "Vui lÃ²ng chá»n má»™t thÃ´ng bÃ¡o Ä‘á»ƒ chá»‰nh sá»­a.");
        put(values, "Please select an announcement to delete.", "Vui lÃ²ng chá»n má»™t thÃ´ng bÃ¡o Ä‘á»ƒ xÃ³a.");
        put(values, "Are you sure you want to remove this announcement from the homepage?", "Báº¡n cÃ³ cháº¯c muá»‘n gá»¡ thÃ´ng bÃ¡o nÃ y khá»i trang chá»§ khÃ´ng?");
        put(values, "Homepage announcement deleted successfully.", "ÄÃ£ xÃ³a thÃ´ng bÃ¡o trang chá»§ thÃ nh cÃ´ng.");
        put(values, "Please select an auction to use its schedule.", "Vui lÃ²ng chá»n má»™t phiÃªn Ä‘á»ƒ dÃ¹ng lá»‹ch cá»§a nÃ³.");
        put(values, "Homepage announcement updated successfully.", "ÄÃ£ cáº­p nháº­t thÃ´ng bÃ¡o trang chá»§ thÃ nh cÃ´ng.");
        put(values, "Bid placement is not ready.", "Chá»©c nÄƒng Ä‘áº·t giÃ¡ hiá»‡n chÆ°a sáºµn sÃ ng.");
        put(values, "Please select an auction.", "Vui lÃ²ng chá»n má»™t phiÃªn.");
        put(values, "Please enter a bid amount.", "Vui lÃ²ng nháº­p má»©c giÃ¡ Ä‘áº¥u.");
        put(values, "Please enter a bid amount before placing a bid.", "Vui lÃ²ng nháº­p má»©c giÃ¡ trÆ°á»›c khi Ä‘áº·t giÃ¡.");
        put(values, "Submitting your bid...", "Äang gá»­i giÃ¡ Ä‘áº¥u cá»§a báº¡n...");
        put(values, "Bid placed successfully. Refreshing the selected auction.", "Äáº·t giÃ¡ thÃ nh cÃ´ng. Äang lÃ m má»›i phiÃªn Ä‘Ã£ chá»n.");
        put(values, "Bid placed successfully.", "Äáº·t giÃ¡ thÃ nh cÃ´ng.");
        put(values, "Unable to place a bid right now.", "Hiá»‡n khÃ´ng thá»ƒ Ä‘áº·t giÃ¡.");
        put(values, "Invalid amount.", "Sá»‘ tiá»n khÃ´ng há»£p lá»‡.");
        put(values, "AuctionController has not been assigned to the bidder screen.", "AuctionController chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh ngÆ°á»i Ä‘áº¥u giÃ¡.");
        put(values, "Connect this controller to AuthFrame to open bid history using FXML.", "HÃ£y káº¿t ná»‘i controller nÃ y vá»›i AuthFrame Ä‘á»ƒ má»Ÿ lá»‹ch sá»­ Ä‘áº¥u giÃ¡ báº±ng FXML.");
        put(values, "You have just been outbid in this auction.", "Báº¡n vá»«a bá»‹ vÆ°á»£t giÃ¡ á»Ÿ phiÃªn nÃ y.");
        put(values, "Outbid", "Bá»‹ vÆ°á»£t giÃ¡");
        put(values, "You have just been outbid in the auction you are watching.", "Báº¡n vá»«a bá»‹ vÆ°á»£t giÃ¡ trong phiÃªn Ä‘ang theo dÃµi.");
        put(values, "Leading", "Äang dáº«n Ä‘áº§u");
        put(values, "You are currently leading.", "Báº¡n hiá»‡n Ä‘ang dáº«n Ä‘áº§u.");
        put(values, "You currently have the highest bid in this auction.", "Báº¡n hiá»‡n Ä‘ang cÃ³ má»©c giÃ¡ cao nháº¥t á»Ÿ phiÃªn nÃ y.");
        put(values, "AuctionController has not been assigned to the seller screen.", "AuctionController chÆ°a Ä‘Æ°á»£c gÃ¡n cho mÃ n hÃ¬nh ngÆ°á»i bÃ¡n.");
        put(values, "Current seller information is unavailable.", "ThÃ´ng tin ngÆ°á»i bÃ¡n hiá»‡n khÃ´ng kháº£ dá»¥ng.");
        put(values, "Auction saved successfully.", "ÄÃ£ lÆ°u phiÃªn Ä‘áº¥u giÃ¡ thÃ nh cÃ´ng.");
        put(values, "Invalid starting price.", "GiÃ¡ khá»Ÿi Ä‘iá»ƒm khÃ´ng há»£p lá»‡.");
        put(values, "Auction deletion is not ready.", "Chá»©c nÄƒng xÃ³a phiÃªn hiá»‡n chÆ°a sáºµn sÃ ng.");
        put(values, "Are you sure you want to delete or cancel this auction?", "Báº¡n cÃ³ cháº¯c muá»‘n xÃ³a hoáº·c há»§y phiÃªn nÃ y khÃ´ng?");
        put(values, "Auction deleted or cancelled successfully.", "ÄÃ£ xÃ³a hoáº·c há»§y phiÃªn thÃ nh cÃ´ng.");
        put(values, "Auction closing is not ready.", "Chá»©c nÄƒng Ä‘Ã³ng phiÃªn hiá»‡n chÆ°a sáºµn sÃ ng.");
        put(values, "Auction closed successfully.", "ÄÃ£ Ä‘Ã³ng phiÃªn thÃ nh cÃ´ng.");
        put(values, "Only admins can issue early-close commands.", "Chá»‰ quáº£n trá»‹ viÃªn má»›i Ä‘Æ°á»£c ra lá»‡nh Ä‘Ã³ng sá»›m.");
        put(values, "Only admins can cancel early-close commands.", "Chá»‰ quáº£n trá»‹ viÃªn má»›i Ä‘Æ°á»£c há»§y lá»‡nh Ä‘Ã³ng sá»›m.");
        put(values, "Only admins can publish announcements to the homepage.", "Chá»‰ quáº£n trá»‹ viÃªn má»›i Ä‘Æ°á»£c Ä‘Äƒng thÃ´ng bÃ¡o lÃªn trang chá»§.");
        put(values, "Only admins can delete homepage announcements.", "Chá»‰ quáº£n trá»‹ viÃªn má»›i Ä‘Æ°á»£c xÃ³a thÃ´ng bÃ¡o trang chá»§.");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add service translations.
    private static void addServiceTranslations(Map<String, String> values) {
        put(values, "Account for lock or unlock was not found.", "KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n Ä‘á»ƒ khÃ³a hoáº·c má»Ÿ khÃ³a.");
        put(values, "Announcement summary cannot be empty.", "TÃ³m táº¯t thÃ´ng bÃ¡o khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Announcement title cannot be empty.", "TiÃªu Ä‘á» thÃ´ng bÃ¡o khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Auction item not found.", "KhÃ´ng tÃ¬m tháº¥y máº·t hÃ ng Ä‘áº¥u giÃ¡.");
        put(values, "Auction not found.", "KhÃ´ng tÃ¬m tháº¥y phiÃªn Ä‘áº¥u giÃ¡.");
        put(values, "Auction schedule information cannot be empty.", "ThÃ´ng tin lá»‹ch phiÃªn khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Cannot create an expired auction.", "KhÃ´ng thá»ƒ táº¡o phiÃªn Ä‘Ã£ háº¿t háº¡n.");
        put(values, "Current password is incorrect.", "Máº­t kháº©u hiá»‡n táº¡i khÃ´ng Ä‘Ãºng.");
        put(values, "Default admin password is fixed and cannot be changed.", "Máº­t kháº©u quáº£n trá»‹ máº·c Ä‘á»‹nh Ä‘Æ°á»£c cá»‘ Ä‘á»‹nh vÃ  khÃ´ng thá»ƒ thay Ä‘á»•i.");
        put(values, "Early-close countdown is only available while the auction is RUNNING.", "Äáº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m chá»‰ kháº£ dá»¥ng khi phiÃªn Ä‘ang RUNNING.");
        put(values, "Email already exists.", "Email Ä‘Ã£ tá»“n táº¡i.");
        put(values, "Full name cannot be empty.", "Há» vÃ  tÃªn khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Homepage announcement not found.", "KhÃ´ng tÃ¬m tháº¥y thÃ´ng bÃ¡o trang chá»§.");
        put(values, "Incorrect username or password.", "TÃªn Ä‘Äƒng nháº­p hoáº·c máº­t kháº©u khÃ´ng Ä‘Ãºng.");
        put(values, "Only the default admin can promote accounts to admin.", "Chá»‰ quáº£n trá»‹ máº·c Ä‘á»‹nh má»›i cÃ³ thá»ƒ nÃ¢ng tÃ i khoáº£n lÃªn quáº£n trá»‹.");
        put(values, "Only the default admin can demote admin accounts.", "Chá»‰ quáº£n trá»‹ máº·c Ä‘á»‹nh má»›i cÃ³ thá»ƒ háº¡ quyá»n quáº£n trá»‹.");
        put(values, "Account for promotion was not found.", "KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n Ä‘á»ƒ nÃ¢ng quyá»n.");
        put(values, "Account for demotion was not found.", "KhÃ´ng tÃ¬m tháº¥y tÃ i khoáº£n Ä‘á»ƒ háº¡ quyá»n.");
        put(values, "The default admin account is already the super admin.", "TÃ i khoáº£n quáº£n trá»‹ máº·c Ä‘á»‹nh Ä‘Ã£ lÃ  super admin.");
        put(values, "The default admin account cannot be demoted.", "KhÃ´ng thá»ƒ háº¡ quyá»n tÃ i khoáº£n quáº£n trá»‹ máº·c Ä‘á»‹nh.");
        put(values, "Account is already an admin.", "TÃ i khoáº£n nÃ y Ä‘Ã£ lÃ  quáº£n trá»‹ viÃªn.");
        put(values, "Only admin accounts can be demoted.", "Chá»‰ tÃ i khoáº£n quáº£n trá»‹ má»›i cÃ³ thá»ƒ bá»‹ háº¡ quyá»n.");
        put(values, "Account promoted to admin successfully.", "ÄÃ£ nÃ¢ng tÃ i khoáº£n lÃªn quáº£n trá»‹ viÃªn thÃ nh cÃ´ng.");
        put(values, "Admin account demoted successfully.", "ÄÃ£ háº¡ quyá»n tÃ i khoáº£n quáº£n trá»‹ thÃ nh cÃ´ng.");
        put(values, "PROMOTE ADMIN", "NÃ‚NG LÃŠN QUáº¢N TRá»Š");
        put(values, "DEMOTE ADMIN", "Háº  QUYá»€N QUáº¢N TRá»Š");
        put(values, "Invalid password. It must be at least 6 characters long and include letters and numbers.", "Máº­t kháº©u khÃ´ng há»£p lá»‡. Máº­t kháº©u pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘.");
        put(values, "Invalid username. It must be 3 to 20 characters long and cannot be empty.", "TÃªn Ä‘Äƒng nháº­p khÃ´ng há»£p lá»‡. TÃªn Ä‘Äƒng nháº­p pháº£i dÃ i tá»« 3 Ä‘áº¿n 20 kÃ½ tá»± vÃ  khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Item not found.", "KhÃ´ng tÃ¬m tháº¥y má»¥c.");
        put(values, "New password must be at least 6 characters and include letters and numbers.", "Máº­t kháº©u má»›i pháº£i cÃ³ Ã­t nháº¥t 6 kÃ½ tá»± vÃ  gá»“m cáº£ chá»¯ láº«n sá»‘.");
        put(values, "Only admins can lock or unlock accounts.", "Chá»‰ quáº£n trá»‹ viÃªn má»›i Ä‘Æ°á»£c khÃ³a hoáº·c má»Ÿ khÃ³a tÃ i khoáº£n.");
        put(values, "Only the creator can delete or cancel this item.", "Chá»‰ ngÆ°á»i táº¡o má»›i Ä‘Æ°á»£c xÃ³a hoáº·c há»§y má»¥c nÃ y.");
        put(values, "Only the creator can edit this item.", "Chá»‰ ngÆ°á»i táº¡o má»›i Ä‘Æ°á»£c chá»‰nh sá»­a má»¥c nÃ y.");
        put(values, "Please enter your username and password.", "Vui lÃ²ng nháº­p tÃªn Ä‘Äƒng nháº­p vÃ  máº­t kháº©u.");
        put(values, "Product name cannot be empty.", "TÃªn sáº£n pháº©m khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng.");
        put(values, "Start time must be earlier than end time.", "Thá»i gian báº¯t Ä‘áº§u pháº£i sá»›m hÆ¡n thá»i gian káº¿t thÃºc.");
        put(values, "Starting price must be greater than 0.", "GiÃ¡ khá»Ÿi Ä‘iá»ƒm pháº£i lá»›n hÆ¡n 0.");
        put(values, "The auction has already ended or was cancelled.", "PhiÃªn Ä‘áº¥u giÃ¡ Ä‘Ã£ káº¿t thÃºc hoáº·c Ä‘Ã£ bá»‹ há»§y.");
        put(values, "The auction is not currently running.", "PhiÃªn Ä‘áº¥u giÃ¡ hiá»‡n khÃ´ng cháº¡y.");
        put(values, "The current time is not valid for bidding.", "Thá»i Ä‘iá»ƒm hiá»‡n táº¡i khÃ´ng há»£p lá»‡ Ä‘á»ƒ Ä‘áº¥u giÃ¡.");
        put(values, "This auction has not activated the early-close countdown.", "PhiÃªn nÃ y chÆ°a kÃ­ch hoáº¡t Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m.");
        put(values, "This auction is already in an early-close countdown process.", "PhiÃªn nÃ y Ä‘Ã£ á»Ÿ trong quÃ¡ trÃ¬nh Ä‘áº¿m ngÆ°á»£c Ä‘Ã³ng sá»›m.");
        put(values, "This item already has bids and can no longer be edited.", "Má»¥c nÃ y Ä‘Ã£ cÃ³ lÆ°á»£t Ä‘áº¥u giÃ¡ vÃ  khÃ´ng thá»ƒ chá»‰nh sá»­a ná»¯a.");
        put(values, "This item can only be edited before it starts or while it is in OPEN status.", "Má»¥c nÃ y chá»‰ cÃ³ thá»ƒ chá»‰nh sá»­a trÆ°á»›c khi báº¯t Ä‘áº§u hoáº·c khi Ä‘ang á»Ÿ tráº¡ng thÃ¡i OPEN.");
        put(values, "User not found.", "KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng.");
        put(values, "Username already exists.", "TÃªn Ä‘Äƒng nháº­p Ä‘Ã£ tá»“n táº¡i.");
        put(values, "You cannot lock your own account.", "Báº¡n khÃ´ng thá»ƒ khÃ³a chÃ­nh tÃ i khoáº£n cá»§a mÃ¬nh.");
        put(values, "You do not have permission to close this auction.", "Báº¡n khÃ´ng cÃ³ quyá»n Ä‘Ã³ng phiÃªn nÃ y.");
        put(values, "Your account has been locked.", "TÃ i khoáº£n cá»§a báº¡n Ä‘Ã£ bá»‹ khÃ³a.");
    }
    // Phuong thuc: tao, mo, hien thi hoac bo sung du lieu cho thao tac add database translations.
    private static void addDatabaseTranslations(Map<String, String> values) {
        put(values, "PostgreSQL JDBC driver not found. Make sure the PostgreSQL driver is available.", "KhÃ´ng tÃ¬m tháº¥y driver JDBC PostgreSQL. HÃ£y báº£o Ä‘áº£m driver PostgreSQL Ä‘Ã£ sáºµn sÃ ng.");
        put(values, "The PostgreSQL connection pool is exhausted. Please try again.", "Pool káº¿t ná»‘i PostgreSQL Ä‘Ã£ háº¿t. Vui lÃ²ng thá»­ láº¡i.");
        put(values, "Interrupted while waiting for a PostgreSQL connection.", "ÄÃ£ bá»‹ ngáº¯t khi Ä‘ang chá» káº¿t ná»‘i PostgreSQL.");
        put(values, "PostgreSQL connection is closed.", "Káº¿t ná»‘i PostgreSQL Ä‘Ã£ Ä‘Ã³ng.");
        put(values, "Unable to initialize the PostgreSQL connection.", "KhÃ´ng thá»ƒ khá»Ÿi táº¡o káº¿t ná»‘i PostgreSQL.");
        put(values, "Unable to read the database configuration file.", "KhÃ´ng thá»ƒ Ä‘á»c tá»‡p cáº¥u hÃ¬nh cÆ¡ sá»Ÿ dá»¯ liá»‡u.");
        put(values, "Unable to save the user to PostgreSQL.", "KhÃ´ng thá»ƒ lÆ°u ngÆ°á»i dÃ¹ng vÃ o PostgreSQL.");
        put(values, "Unable to update the user in PostgreSQL.", "KhÃ´ng thá»ƒ cáº­p nháº­t ngÆ°á»i dÃ¹ng trong PostgreSQL.");
        put(values, "Unable to find the user by username in PostgreSQL.", "KhÃ´ng thá»ƒ tÃ¬m ngÆ°á»i dÃ¹ng theo tÃªn Ä‘Äƒng nháº­p trong PostgreSQL.");
        put(values, "Unable to find the user by email in PostgreSQL.", "KhÃ´ng thá»ƒ tÃ¬m ngÆ°á»i dÃ¹ng theo email trong PostgreSQL.");
        put(values, "Unable to read the user list from PostgreSQL.", "KhÃ´ng thá»ƒ Ä‘á»c danh sÃ¡ch ngÆ°á»i dÃ¹ng tá»« PostgreSQL.");
        put(values, "Unable to save the auction to PostgreSQL.", "KhÃ´ng thá»ƒ lÆ°u phiÃªn Ä‘áº¥u giÃ¡ vÃ o PostgreSQL.");
        put(values, "Unable to update the auction in PostgreSQL.", "KhÃ´ng thá»ƒ cáº­p nháº­t phiÃªn Ä‘áº¥u giÃ¡ trong PostgreSQL.");
        put(values, "Unable to delete the auction in PostgreSQL.", "KhÃ´ng thá»ƒ xÃ³a phiÃªn Ä‘áº¥u giÃ¡ trong PostgreSQL.");
        put(values, "Unable to find the auction in PostgreSQL.", "KhÃ´ng thá»ƒ tÃ¬m phiÃªn Ä‘áº¥u giÃ¡ trong PostgreSQL.");
        put(values, "Unable to read the auction list from PostgreSQL.", "KhÃ´ng thá»ƒ Ä‘á»c danh sÃ¡ch phiÃªn Ä‘áº¥u giÃ¡ tá»« PostgreSQL.");
        put(values, "Unable to save the bid transaction to PostgreSQL.", "KhÃ´ng thá»ƒ lÆ°u giao dá»‹ch Ä‘áº¥u giÃ¡ vÃ o PostgreSQL.");
        put(values, "Unable to read bid history from PostgreSQL.", "KhÃ´ng thá»ƒ Ä‘á»c lá»‹ch sá»­ Ä‘áº¥u giÃ¡ tá»« PostgreSQL.");
        put(values, "Unable to read all bid transactions from PostgreSQL.", "KhÃ´ng thá»ƒ Ä‘á»c toÃ n bá»™ giao dá»‹ch Ä‘áº¥u giÃ¡ tá»« PostgreSQL.");
        put(values, "Unable to save homepage content to PostgreSQL.", "KhÃ´ng thá»ƒ lÆ°u ná»™i dung trang chá»§ vÃ o PostgreSQL.");
        put(values, "Unable to update homepage content in PostgreSQL.", "KhÃ´ng thá»ƒ cáº­p nháº­t ná»™i dung trang chá»§ trong PostgreSQL.");
        put(values, "Unable to delete homepage content in PostgreSQL.", "KhÃ´ng thá»ƒ xÃ³a ná»™i dung trang chá»§ trong PostgreSQL.");
        put(values, "Unable to find the homepage announcement in PostgreSQL.", "KhÃ´ng thá»ƒ tÃ¬m thÃ´ng bÃ¡o trang chá»§ trong PostgreSQL.");
        put(values, "Unable to read homepage announcements from PostgreSQL.", "KhÃ´ng thá»ƒ Ä‘á»c thÃ´ng bÃ¡o trang chá»§ tá»« PostgreSQL.");
    }

    private static Map<String, String> createReverseTranslations(Map<String, String> source) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            values.put(entry.getValue(), entry.getKey());
        }
        return values;
    }
    // Phuong thuc: thuc hien chuc nang put trong lop UiText.
    private static void put(Map<String, String> values, String source, String target) {
        values.put(source, target);
    }
}
