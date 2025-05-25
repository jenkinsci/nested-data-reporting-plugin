package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.PieChartModel;
import edu.hm.hafner.echarts.PieData;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.LinkedHashMap;
import java.util.List; // Moved import
import java.util.Optional; // Moved import

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ItemPieChartTest {

    @Mock
    private Report report;

    @Mock
    private Item item;

    private ItemPieChart itemPieChart;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemPieChart = new ItemPieChart();
        when(item.getId()).thenReturn("test-item"); // Ensure item has an ID for PieChartModel constructor
    }

    @Test
    void testCreate_displayTypeDiff_returnsPlaceholderModel() {
        when(report.getDisplayType()).thenReturn(DisplayType.DIFF);
        when(report.getColor("default")).thenReturn("grey"); // Mock color for placeholder

        PieChartModel model = itemPieChart.create(report, item);

        assertThat(model.getName()).isEqualTo("test-item");
        assertThat(model.getData()).hasSize(1); // Reverted to getData()
        PieData pieData = model.getData().get(0); // Reverted to getData()
        assertThat(pieData.getName()).isEqualTo("Diff display not applicable for pie chart");
        assertThat(pieData.getValue()).isEqualTo(1);
        assertThat(model.getColors()).hasSize(1);
        assertThat(model.getColors().get(0)).isEqualTo("grey");
    }

    @Test
    void testCreate_displayTypeAbsolute_returnsCorrectModel() {
        when(report.getDisplayType()).thenReturn(DisplayType.ABSOLUTE);
        LinkedHashMap<String, Integer> results = new LinkedHashMap<>();
        results.put("Passed", 50);
        results.put("Failed", 10);
        when(item.getResult()).thenReturn(results);

        // Mock color lookups
        when(report.getColor("Passed")).thenReturn("green");
        when(report.getColor("Failed")).thenReturn("red");

        PieChartModel model = itemPieChart.create(report, item);

        assertThat(model.getName()).isEqualTo("test-item");
        assertThat(model.getData()).hasSize(2); // Reverted to getData()
        assertThat(model.getColors()).hasSize(2);

        List<PieData> seriesData = model.getData(); // Reverted to getData()
        List<String> colorList = model.getColors();

        Optional<PieData> passedDataOptional = seriesData.stream().filter(pd -> "Passed".equals(pd.getName())).findFirst();
        assertThat(passedDataOptional).isPresent();
        if (passedDataOptional.isPresent()) {
            PieData passedData = passedDataOptional.get();
            assertThat(passedData.getValue()).isEqualTo(50);
            int passedDataIndex = seriesData.indexOf(passedData);
            assertThat(colorList.get(passedDataIndex)).isEqualTo("green");
        }

        Optional<PieData> failedDataOptional = seriesData.stream().filter(pd -> "Failed".equals(pd.getName())).findFirst();
        assertThat(failedDataOptional).isPresent();
        if (failedDataOptional.isPresent()) {
            PieData failedData = failedDataOptional.get();
            assertThat(failedData.getValue()).isEqualTo(10);
            int failedDataIndex = seriesData.indexOf(failedData);
            assertThat(colorList.get(failedDataIndex)).isEqualTo("red");
        }
    }
    
    @Test
    void testCreate_displayTypeRelative_returnsCorrectModel() {
        when(report.getDisplayType()).thenReturn(DisplayType.RELATIVE);
        LinkedHashMap<String, Integer> results = new LinkedHashMap<>();
        results.put("Success", 75);
        results.put("Error", 25);
        when(item.getResult()).thenReturn(results);
        when(item.getTotal()).thenReturn(100); // Total is needed for relative calculation if ItemPieChart uses it.
                                                // Currently, ItemPieChart directly uses the result values.

        when(report.getColor("Success")).thenReturn("blue");
        when(report.getColor("Error")).thenReturn("orange");

        PieChartModel model = itemPieChart.create(report, item);

        assertThat(model.getName()).isEqualTo("test-item");
        assertThat(model.getData()).hasSize(2); // Reverted to getData()
        assertThat(model.getColors()).hasSize(2);
        
        List<PieData> seriesDataRelative = model.getData(); // Reverted to getData()
        List<String> colorListRelative = model.getColors();

        Optional<PieData> successDataOptional = seriesDataRelative.stream().filter(pd -> "Success".equals(pd.getName())).findFirst();
        assertThat(successDataOptional).isPresent();
        if (successDataOptional.isPresent()) {
            PieData successData = successDataOptional.get();
            assertThat(successData.getValue()).isEqualTo(75); // Values are absolute in PieChart
            int successDataIndex = seriesDataRelative.indexOf(successData);
            assertThat(colorListRelative.get(successDataIndex)).isEqualTo("blue");
        }

        Optional<PieData> errorDataOptional = seriesDataRelative.stream().filter(pd -> "Error".equals(pd.getName())).findFirst();
        assertThat(errorDataOptional).isPresent();
        if (errorDataOptional.isPresent()) {
            PieData errorData = errorDataOptional.get();
            assertThat(errorData.getValue()).isEqualTo(25);
            int errorDataIndex = seriesDataRelative.indexOf(errorData);
            assertThat(colorListRelative.get(errorDataIndex)).isEqualTo("orange");
        }
    }
} // Added closing brace for the class
