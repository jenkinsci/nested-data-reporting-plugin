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
    protected Map<String, Integer> computeSeries(ReportResult reportResult) {

        Map<String, Double> doubleResult = reportResult.getReport().aggregate();
        Map<String, Integer> result = doubleResult.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue(), (v1, v2) -> v1, java.util.LinkedHashMap::new));

        if (result.size() == 1) {
            // If the aggregated result has only one entry, the original logic was to then return totals of individual items.
            // This seems to imply that if the aggregate is a single value, perhaps a different view is desired.
            // We need to ensure this path also returns Map<String, Integer>.
            return reportResult.getReport().getItems().stream()
                    .collect(Collectors.toMap(Item::getId, item -> (int) item.getTotal(), (v1, v2) -> v1, java.util.LinkedHashMap::new));
        }

        return result;
    }

}