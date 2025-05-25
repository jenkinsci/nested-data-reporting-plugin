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
        
        // Adjust formatting for RELATIVE and DUAL as per existing Item.java logic
        String actualLabel;
        if (displayType == DisplayType.RELATIVE) {
            actualLabel = String.format("%.2f%%", percentage * 100); // Original logic seems to expect percentage * 100
        } else if (displayType == DisplayType.DUAL) {
             actualLabel = String.format("%s (%.2f%%)", String.valueOf(value), percentage * 100); // Original logic
        }
        else {
            actualLabel = item.getLabel(report, value, percentage);
        }
        
        // The Item.getLabel formats percentage * 100 for RELATIVE and DUAL.
        // The CsvSource has '0.5' for 50%. The original code's getLabel for RELATIVE takes 'percentage' and formats it as `String.format("%.2f%%", percentage)`.
        // This means if I pass 0.5, it becomes "0.50%".
        // If the original code expected 'percentage' to be already like 50.0 for 50%, then my CsvSource is fine.
        // Let's re-check Item.java's getLabel:
        // DUAL: return String.format("%s (%.2f%%)", value.toString(), percentage);
        // RELATIVE: return String.format("%.2f%%", percentage);
        // This implies 'percentage' passed to getLabel should be the direct value like 50.0 for 50%, not 0.5.
        // The test CSV has 0.5. If this is interpreted as 0.5%, it's too small.
        // The problem description has "0.50%". If the input 'percentage' is 0.5, then `String.format("%.2f%%", 0.5)` yields "0.50%".
        // If the input 'percentage' is 50.0, then `String.format("%.2f%%", 50.0)` yields "50.00%".
        // The existing Item.java code is:
        // `String.format("%s (%.2f%%)", value.toString(), percentage);` for DUAL
        // `String.format("%.2f%%", percentage);` for RELATIVE
        // This means the `percentage` parameter itself should be the value to be formatted, e.g. 50.0 for 50%.
        // My CsvSource for percentage is `0.5`. So, `String.format("%.2f%%", 0.5)` results in `0.50%`.
        // This matches my `expectedLabel` for RELATIVE and DUAL.
        
        // Re-evaluating the CsvSource and expectedLabel based on Item.java:
        // If DisplayType.RELATIVE, item.getLabel(report, 10, 0.5) -> String.format("%.2f%%", 0.5) -> "0.50%"
        // If DisplayType.DUAL, item.getLabel(report, 10, 0.5) -> String.format("%s (%.2f%%)", "10", 0.5) -> "10 (0.50%)"
        // This seems correct. My CsvSource is providing the 'percentage' argument as a direct value to be formatted.

        assertThat(item.getLabel(report, value, percentage)).isEqualTo(expectedLabel);
    }
}
