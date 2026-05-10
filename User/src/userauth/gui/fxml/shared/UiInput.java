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

    public static void installMoneyInput(TextField textField) {
        if (textField == null) {
            return;
        }
        textField.setTextFormatter(new TextFormatter<>(moneyGroupingFilter()));
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

    static String formatMoneyInputText(String rawValue) {
        String digits = digitsOnly(rawValue);
        if (digits.isBlank()) {
            return "";
        }
        return addThousandsDots(stripLeadingZeros(digits));
    }

    private static UnaryOperator<TextFormatter.Change> decimalFilter() {
        return change -> change.getControlNewText().matches("[0-9., ]*") ? change : null;
    }

    private static UnaryOperator<TextFormatter.Change> moneyGroupingFilter() {
        return change -> {
            if (!change.isContentChange()) {
                return change;
            }

            MoneyEdit edit = formatMoneyEdit(
                    change.getControlText(),
                    change.getRangeStart(),
                    change.getRangeEnd(),
                    change.getText()
            );
            if (edit == null) {
                return null;
            }

            change.setRange(0, change.getControlText().length());
            change.setText(edit.text());
            change.setCaretPosition(edit.caretPosition());
            change.setAnchor(edit.caretPosition());
            return change;
        };
    }

    private static UnaryOperator<TextFormatter.Change> integerFilter() {
        return change -> change.getControlNewText().matches("[0-9]*") ? change : null;
    }

    private static String digitsOnly(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) {
                digits.append(ch);
            }
        }
        return digits.toString();
    }

    private static String stripLeadingZeros(String digits) {
        int firstNonZero = 0;
        while (firstNonZero < digits.length() - 1 && digits.charAt(firstNonZero) == '0') {
            firstNonZero++;
        }
        return digits.substring(firstNonZero);
    }

    private static String addThousandsDots(String digits) {
        StringBuilder formatted = new StringBuilder(digits);
        for (int index = formatted.length() - 3; index > 0; index -= 3) {
            formatted.insert(index, '.');
        }
        return formatted.toString();
    }

    private static int countDigitsBeforeCaret(String value, int caretPosition) {
        int limit = Math.min(Math.max(caretPosition, 0), value.length());
        int count = 0;
        for (int i = 0; i < limit; i++) {
            if (Character.isDigit(value.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private static int caretPositionAfterDigits(String value, int digitCount) {
        if (digitCount <= 0) {
            return 0;
        }
        int seen = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                seen++;
                if (seen == digitCount) {
                    return i + 1;
                }
            }
        }
        return value.length();
    }

    static MoneyEdit formatMoneyEdit(String controlText, int rangeStart, int rangeEnd, String replacement) {
        String currentText = controlText == null ? "" : controlText;
        String replacementText = replacement == null ? "" : replacement;
        int safeStart = Math.min(Math.max(rangeStart, 0), currentText.length());
        int safeEnd = Math.min(Math.max(rangeEnd, safeStart), currentText.length());

        String replacementDigits = digitsOnly(replacementText);
        if (!replacementText.isEmpty() && replacementDigits.isEmpty()) {
            return null;
        }

        String currentDigits = digitsOnly(currentText);
        int startDigit = countDigitsBeforeCaret(currentText, safeStart);
        int endDigit = countDigitsBeforeCaret(currentText, safeEnd);
        String editedDigits = currentDigits.substring(0, startDigit)
                + replacementDigits
                + currentDigits.substring(endDigit);
        if (editedDigits.isEmpty()) {
            return new MoneyEdit("", 0);
        }

        int caretDigit = startDigit + replacementDigits.length();
        String strippedDigits = stripLeadingZeros(editedDigits);
        int removedLeadingZeros = editedDigits.length() - strippedDigits.length();
        int adjustedCaretDigit = Math.max(0, caretDigit - removedLeadingZeros);
        String formatted = addThousandsDots(strippedDigits);
        return new MoneyEdit(formatted, caretPositionAfterDigits(formatted, adjustedCaretDigit));
    }

    record MoneyEdit(String text, int caretPosition) {}
}
