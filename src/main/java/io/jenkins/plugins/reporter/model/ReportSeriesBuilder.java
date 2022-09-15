package io.jenkins.plugins.reporter.model;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds one x-axis point for the series of a line chart showing the parts 
 * of a report from json model. The results of all items are aggregated.
 *
 * @author Simon Symhoven
 */
public class ReportSeriesBuilder extends SeriesBuilder<ReportAction> {

    @Override
    protected Map<String, Integer> computeSeries(ReportAction reportAction) {

        Map<String, Integer> result = reportAction.getResult().getReport().aggregate();

        if (result.size() == 1) {
            return reportAction.getResult().getReport().getItems().stream()
                    .collect(Collectors.toMap(Item::getId, Item::getTotal));
        }

        return result;
    }

}