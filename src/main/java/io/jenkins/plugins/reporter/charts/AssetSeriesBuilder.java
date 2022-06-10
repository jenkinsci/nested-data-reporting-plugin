package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportBuildAction;
import io.jenkins.plugins.reporter.model.Asset;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds one x-axis point for the series of a line chart showing the accurate, 
 * manually and incorrect parts of an asset from the csv file.
 *
 * @author Simon Symhoven
 */
public class AssetSeriesBuilder extends SeriesBuilder<ReportBuildAction> {
    
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
    protected Map<String, Integer> computeSeries(ReportBuildAction dataReportBuildAction) {
        Map<String, Integer> series = new HashMap<>();

        Asset asset = dataReportBuildAction.getResult().getAssets()
                .stream().filter(a -> a.getId().equals(id)).findFirst().get();

        series.put(ACCURATE, asset.getAccurate());
        series.put(MANUALLY, asset.getManually());
        series.put(INCORRECT, asset.getIncorrect());
        
        return series;
    }
}