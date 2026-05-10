package userauth.gui.fxml.shared;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.Objects;
import java.util.function.UnaryOperator;

public final class UiInput {
    private UiInput() {
    }

    public static void installDecimalInput(TextField textField) {
        if (textField == null) {
            return;
        }
        textField.setTextFormatter(new TextFormatter<>(decimalFilter()));
    }

    public static void installPositiveIntegerInput(TextField textField) {
        if (textField == null) {
            return;
        }
        textField.setTextFormatter(new TextFormatter<>(integerFilter()));
    }

    public static double parseDecimal(String rawValue) {
        String normalized = normalizeDecimal(rawValue);
        if (normalized.isBlank()) {
            throw new NumberFormatException("Number is required.");
        }
        return Double.parseDouble(normalized);
    }

    public static double parsePositiveDecimal(String rawValue, String fieldName) {
        double value = parseDecimal(rawValue);
        if (value <= 0) {
            throw new NumberFormatException(Objects.requireNonNullElse(fieldName, "Value") + " must be greater than 0.");
        }
        return value;
    }

    public static int parsePositiveInteger(String rawValue, String fieldName) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        if (normalized.isBlank()) {
            throw new NumberFormatException(Objects.requireNonNullElse(fieldName, "Value") + " is required.");
        }
        int value = Integer.parseInt(normalized);
        if (value <= 0) {
            throw new NumberFormatException(Objects.requireNonNullElse(fieldName, "Value") + " must be greater than 0.");
        }
        return value;
    }

    static String normalizeDecimal(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim().replace(" ", "");
        if (value.isBlank()) {
            return "";
        }

        int lastDot = value.lastIndexOf('.');
        int lastComma = value.lastIndexOf(',');
        if (lastDot >= 0 && lastComma >= 0) {
            char decimalSeparator = lastDot > lastComma ? '.' : ',';
            char groupingSeparator = decimalSeparator == '.' ? ',' : '.';
            return value
                    .replace(String.valueOf(groupingSeparator), "")
                    .replace(decimalSeparator, '.');
        }

        if (lastDot >= 0) {
            return normalizeSingleSeparator(value, '.');
        }
        if (lastComma >= 0) {
            return normalizeSingleSeparator(value, ',');
        }
        return value;
    }

    private static String normalizeSingleSeparator(String value, char separator) {
        long separatorCount = value.chars().filter(ch -> ch == separator).count();
        if (separatorCount > 1) {
            String[] groups = value.split("\\" + separator, -1);
            if (isGroupedThousands(groups)) {
                return value.replace(String.valueOf(separator), "");
            }
            int lastSeparator = value.lastIndexOf(separator);
            String integerPart = value.substring(0, lastSeparator).replace(String.valueOf(separator), "");
            String decimalPart = value.substring(lastSeparator + 1);
            return integerPart + "." + decimalPart;
        }

        int separatorIndex = value.indexOf(separator);
        String before = value.substring(0, separatorIndex);
        String after = value.substring(separatorIndex + 1);
        if (before.length() >= 1 && before.length() <= 3 && after.length() == 3) {
            return before + after;
        }
        return before + "." + after;
    }

    private static boolean isGroupedThousands(String[] groups) {
        if (groups.length < 2 || groups[0].isBlank() || groups[0].length() > 3) {
            return false;
        }
        for (int i = 1; i < groups.length; i++) {
            if (groups[i].length() != 3 || !groups[i].chars().allMatch(Character::isDigit)) {
                return false;
            }
        }
        return groups[0].chars().allMatch(Character::isDigit);
    }

    private static UnaryOperator<TextFormatter.Change> decimalFilter() {
        return change -> change.getControlNewText().matches("[0-9., ]*") ? change : null;
    }

    private static UnaryOperator<TextFormatter.Change> integerFilter() {
        return change -> change.getControlNewText().matches("[0-9]*") ? change : null;
    }
}
