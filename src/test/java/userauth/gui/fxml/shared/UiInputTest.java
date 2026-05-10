package userauth.gui.fxml.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
