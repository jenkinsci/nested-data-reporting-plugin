package io.jenkins.plugins.reporter.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemTest {

    @Test
    void getLabel_displayTypeDiff_formatsPositiveValueCorrectly() {
        Report report = mock(Report.class);
        when(report.getDisplayType()).thenReturn(DisplayType.DIFF);
        Item item = new Item();
        assertThat(item.getLabel(report, 5, 0.0)).isEqualTo("+5");
    }

    @Test
    void getLabel_displayTypeDiff_formatsNegativeValueCorrectly() {
        Report report = mock(Report.class);
        when(report.getDisplayType()).thenReturn(DisplayType.DIFF);
        Item item = new Item();
        assertThat(item.getLabel(report, -3, 0.0)).isEqualTo("-3");
    }

    @Test
    void getLabel_displayTypeDiff_formatsZeroValueCorrectly() {
        Report report = mock(Report.class);
        when(report.getDisplayType()).thenReturn(DisplayType.DIFF);
        Item item = new Item();
        assertThat(item.getLabel(report, 0, 0.0)).isEqualTo("+0");
    }
    
    @ParameterizedTest(name = "[{index}] DisplayType={0}, Value={1}, Percentage={2} => Expected Label=''{3}''")
    @CsvSource({
            "ABSOLUTE, 10, 0.5, '10'",
            "RELATIVE, 10, 0.5, '0.50%'", // Assuming percentage is 0.0 to 1.0, formatted to two decimal places
            "DUAL,    10, 0.5, '10 (0.50%)'"
    })
    void getLabel_otherDisplayTypes_formatsCorrectly(String displayTypeStr, int value, double percentage, String expectedLabel) {
        Report report = mock(Report.class);
        DisplayType displayType = DisplayType.valueOf(displayTypeStr);
        when(report.getDisplayType()).thenReturn(displayType);
        
        Item item = new Item();
        
        // The CsvSource for 'percentage' is 0.5.
        // Item.java's getLabel method:
        // - For DUAL: String.format("%s (%.2f%%)", value.toString(), percentage) -> "10 (0.50%)" if value=10, percentage=0.5
        // - For RELATIVE: String.format("%.2f%%", percentage) -> "0.50%" if percentage=0.5
        // - For ABSOLUTE: value.toString() -> "10" if value=10
        // The expectedLabel in CsvSource already matches this behavior.
        assertThat(item.getLabel(report, value, percentage)).isEqualTo(expectedLabel);
    }
}
