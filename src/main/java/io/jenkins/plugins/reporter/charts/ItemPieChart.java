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
            item.getItems().forEach(i -> model.add(new PieData(i.getName(), i.getTotal()), report.getColor(i.getId())));
        } else {
            item.getResult().forEach((key, value) -> model.add(new PieData(key, value),
                    report.getColor(key)));
        }

        return model;
    }
}
