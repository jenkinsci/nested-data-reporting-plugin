package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds one x-axis point for the series of a line chart showing the accurate, 
 * manually and incorrect parts of a report from the csv file. The results of all assets are summarized.
 *
 * @author Simon Symhoven
 */
public class ReportSeriesBuilder extends SeriesBuilder<ReportAction> {
    
    static final String ACCURATE = "accurate";
    static final String MANUALLY = "manually";
    static final String INCORRECT = "incorrect";
    
    @Override
    protected Map<String, Integer> computeSeries(ReportAction dataReportBuildAction) {
        Map<String, Integer> series = new HashMap<>();

        series.put(ACCURATE, 4);
        series.put(MANUALLY, 5);
        series.put(INCORRECT, 6);
        
        return series;
    }
}
