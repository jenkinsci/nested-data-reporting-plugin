package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds one x-axis point for the series of a line chart showing the accurate, 
 * manually and incorrect parts of an asset from the csv file.
 *
 * @author Simon Symhoven
 */
public class AssetSeriesBuilder extends SeriesBuilder<ReportAction> {
    
    private final String id;
    static final String ACCURATE = "accurate";
    static final String MANUALLY = "manually";
    static final String INCORRECT = "incorrect";

    /**
     * Creates a new {@link AssetSeriesBuilder}.
     * 
     * @param id
     *         the id as string of the asset from the csv file to get the series for.
     */
    public AssetSeriesBuilder(String id) {
        this.id = id;
    }

    @Override
    protected Map<String, Integer> computeSeries(ReportAction dataReportBuildAction) {
        Map<String, Integer> series = new HashMap<>();

        series.put(ACCURATE, 1);
        series.put(MANUALLY, 2);
        series.put(INCORRECT, 3);
        
        return series;
    }
}