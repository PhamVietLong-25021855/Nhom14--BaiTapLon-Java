package userauth.gui.fxml.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiInputTest {
    @Test
    void parseDecimalAcceptsCommonThousandsSeparators() {
        assertEquals(1500000.0, UiInput.parseDecimal("1,500,000"));
        assertEquals(1500000.0, UiInput.parseDecimal("1.500.000"));
        assertEquals(1500000.0, UiInput.parseDecimal("1 500 000"));
    }

    @Test
    void parseDecimalAcceptsDecimalSeparatorVariants() {
        assertEquals(1234.5, UiInput.parseDecimal("1,234.5"));
        assertEquals(1234.5, UiInput.parseDecimal("1.234,5"));
    }

    @Test
    void parsePositiveDecimalRejectsEmptyAndNonPositiveValues() {
        assertThrows(NumberFormatException.class, () -> UiInput.parsePositiveDecimal("", "Amount"));
        assertThrows(NumberFormatException.class, () -> UiInput.parsePositiveDecimal("0", "Amount"));
        assertThrows(NumberFormatException.class, () -> UiInput.parsePositiveDecimal("-1", "Amount"));
    }

    @Test
    void formatMoneyInputTextAddsDotThousandsSeparators() {
        assertEquals("1.000", UiInput.formatMoneyInputText("1000"));
        assertEquals("1.500.000", UiInput.formatMoneyInputText("1500000"));
        assertEquals("1.500.000", UiInput.formatMoneyInputText("1,500,000"));
    }

    @Test
    void formatMoneyEditKeepsDigitsStableWhenTypingFromKeyboard() {
        MoneyTyping typing = typeMoney("", "1000000");

        assertEquals("1.000.000", typing.text());
        assertEquals("1000000", typing.text().replace(".", ""));
        assertEquals(typing.text().length(), typing.caretPosition());
    }

    @Test
    void formatMoneyEditHandlesTypingAfterGroupedText() {
        UiInput.MoneyEdit edit = UiInput.formatMoneyEdit("1.000", 5, 5, "0");

        assertEquals("10.000", edit.text());
        assertEquals(6, edit.caretPosition());
    }

    @Test
    void formatMoneyEditRejectsNonDigitKeyboardInput() {
        assertNull(UiInput.formatMoneyEdit("1.000", 5, 5, "a"));
    }

    @Test
    void formatMoneyEditHandlesBackspaceAtEndOfGroupedText() {
        UiInput.MoneyEdit edit = UiInput.formatMoneyEdit("1.000", 4, 5, "", 5);

        assertEquals("100", edit.text());
        assertEquals(3, edit.caretPosition());
    }

    @Test
    void formatMoneyEditHandlesBackspaceNextToGroupingDot() {
        UiInput.MoneyEdit edit = UiInput.formatMoneyEdit("1.000", 1, 2, "", 2);

        assertEquals("0", edit.text());
        assertEquals(0, edit.caretPosition());
    }

    @Test
    void formatMoneyEditHandlesDeleteNextToGroupingDot() {
        UiInput.MoneyEdit edit = UiInput.formatMoneyEdit("1.000", 1, 2, "", 1);

        assertEquals("100", edit.text());
        assertEquals(1, edit.caretPosition());
    }

    private MoneyTyping typeMoney(String initialText, String typedCharacters) {
        String text = initialText;
        int caretPosition = text.length();
        for (int i = 0; i < typedCharacters.length(); i++) {
            UiInput.MoneyEdit edit = UiInput.formatMoneyEdit(
                    text,
                    caretPosition,
                    caretPosition,
                    String.valueOf(typedCharacters.charAt(i))
            );
            text = edit.text();
            caretPosition = edit.caretPosition();
        }
        return new MoneyTyping(text, caretPosition);
    }

    private record MoneyTyping(String text, int caretPosition) {}
}
