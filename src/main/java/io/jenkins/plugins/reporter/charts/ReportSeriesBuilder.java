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
    
    private final Report report;
    
     public ReportSeriesBuilder(Report report) {
        this.report = report;
    }
    
    @Override
    protected Map<String, Integer> computeSeries(ReportAction reportAction) {

        System.out.println(reportAction.getDisplayName());
        
        Map<String, Integer> result = report.getResult().aggregate();
        
        if (result.size() == 1) {
            return report.getResult().getItems().stream()
                    .collect(Collectors.toMap(Item::getId, Item::getTotal));
        }
        
        return reportAction.getReport().getResult().aggregate();
    }
    
}
