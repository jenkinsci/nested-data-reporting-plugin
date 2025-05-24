package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.PieChartModel;
import edu.hm.hafner.echarts.PieData;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;

/**
 * Builds the model for a pie chart showing the attributes of an item.
 *
 * @author Simon Symhoven
 */
public class ItemPieChart {

    /**
     * Creates the chart for the specified item.
     *
     * @param report 
     *          the report of the {@link io.jenkins.plugins.reporter.ReportResult}.
     * @param item
     *         the item to build the pie chart for.
     *         
     * @return the chart model
     */
    public PieChartModel create(Report report, Item item) {
        
        PieChartModel model = new PieChartModel(item.getId());

        if (item.getResult().size() == 1) {
            // item.getResult() has only one entry, typically when values are in sub-items.
            // The original logic implies that if result.size() == 1, we should chart the totals of its children.
            item.getItems().forEach(i -> model.add(new PieData(i.getName(), (int) i.getTotal()), report.getColor(i.getId())));
        } else {
            // item.getResult() has multiple entries, chart these directly.
            item.getResult().forEach((key, value) -> {
                if (value instanceof Number) {
                    model.add(new PieData(key, ((Number) value).intValue()), report.getColor(key));
                } else {
                    // Optional: Log a warning if a non-numeric value is encountered for a chart key
                    // System.err.println("Warning: Non-numeric value for key '" + key + "' in ItemPieChart, value: " + value);
                }
            });
        }

        return model;
    }
}
