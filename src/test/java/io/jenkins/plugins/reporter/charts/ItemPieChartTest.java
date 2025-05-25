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
        assertThat(model.getData()).hasSize(1);
        PieData pieData = model.getData().get(0);
        assertThat(pieData.getName()).isEqualTo("Diff display not applicable for pie chart");
        assertThat(pieData.getValue()).isEqualTo(1);
        assertThat(pieData.getColor()).isEqualTo("grey");
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
        assertThat(model.getData()).hasSize(2);

        PieData passedData = model.getData().stream().filter(d -> d.getName().equals("Passed")).findFirst().orElse(null);
        assertThat(passedData).isNotNull();
        assertThat(passedData.getValue()).isEqualTo(50);
        assertThat(passedData.getColor()).isEqualTo("green");

        PieData failedData = model.getData().stream().filter(d -> d.getName().equals("Failed")).findFirst().orElse(null);
        assertThat(failedData).isNotNull();
        assertThat(failedData.getValue()).isEqualTo(10);
        assertThat(failedData.getColor()).isEqualTo("red");
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
        assertThat(model.getData()).hasSize(2);
        
        PieData successData = model.getData().stream().filter(d -> d.getName().equals("Success")).findFirst().orElse(null);
        assertThat(successData).isNotNull();
        assertThat(successData.getValue()).isEqualTo(75); // Values are absolute in PieChart, labels show percentage if DUAL/RELATIVE
        assertThat(successData.getColor()).isEqualTo("blue");

        PieData errorData = model.getData().stream().filter(d -> d.getName().equals("Error")).findFirst().orElse(null);
        assertThat(errorData).isNotNull();
        assertThat(errorData.getValue()).isEqualTo(25);
        assertThat(errorData.getColor()).isEqualTo("orange");
    }
}
