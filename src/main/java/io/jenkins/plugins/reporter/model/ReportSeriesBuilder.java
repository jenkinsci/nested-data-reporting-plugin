package io.jenkins.plugins.reporter.model;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportResult;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds one x-axis point for the series of a line chart showing the parts 
 * of a report from json model. The results of all items are aggregated.
 *
 * @author Simon Symhoven
 */
public class ReportSeriesBuilder extends SeriesBuilder<ReportResult> {

    @Override
    protected Map<String, Double> computeSeries(ReportResult reportResult) {

        Map<String, Double> result = reportResult.getReport().aggregate();

        if (result.size() == 1) {
            return reportResult.getReport().getItems().stream()
                    .collect(Collectors.toMap(Item::getId, Item::getTotal));
        }

        return result;
    }

}