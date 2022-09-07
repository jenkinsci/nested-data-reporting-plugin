package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;

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
         
        Map<String, Integer> result = reportAction.getReport().getResult().aggregate();
        
        if (result.size() == 1) {
            return reportAction.getReport().getResult().getItems().stream()
                    .collect(Collectors.toMap(Item::getId, Item::getTotal));
        }
        
        return reportAction.getReport().getResult().aggregate();
    }
    
}
