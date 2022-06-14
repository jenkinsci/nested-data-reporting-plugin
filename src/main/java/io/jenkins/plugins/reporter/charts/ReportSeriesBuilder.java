package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;

import java.util.Map;

/**
 * Builds one x-axis point for the series of a line chart showing the parts 
 * of a report from json model. The results of all items are aggregated.
 *
 * @author Simon Symhoven
 */
public class ReportSeriesBuilder extends SeriesBuilder<ReportAction> {
    
    @Override
    protected Map<String, Integer> computeSeries(ReportAction reportAction) {
        return reportAction.getReport().aggregate();
    }
    
}
