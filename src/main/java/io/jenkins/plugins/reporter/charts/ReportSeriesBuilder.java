package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportBuildAction;

import java.util.HashMap;
import java.util.Map;

public class ReportSeriesBuilder extends SeriesBuilder<ReportBuildAction> {
    static final String ACCURATE = "accurate";
    static final String MANUALLY = "manually";
    static final String INCORRECT = "incorrect";
    
    @Override
    protected Map<String, Integer> computeSeries(ReportBuildAction dataReportBuildAction) {
        Map<String, Integer> series = new HashMap<>();

        series.put(ACCURATE, dataReportBuildAction.getResult().getTotalAccurate());
        series.put(MANUALLY, dataReportBuildAction.getResult().getTotalManually());
        series.put(INCORRECT, dataReportBuildAction.getResult().getTotalIncorrect());
        
        return series;
    }
}
