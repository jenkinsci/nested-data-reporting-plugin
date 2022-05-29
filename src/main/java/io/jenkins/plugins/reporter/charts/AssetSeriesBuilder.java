package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportBuildAction;
import io.jenkins.plugins.reporter.model.Asset;

import java.util.HashMap;
import java.util.Map;

public class AssetSeriesBuilder extends SeriesBuilder<ReportBuildAction> {
    private final String id;
    static final String ACCURATE = "accurate";
    static final String MANUALLY = "manually";
    static final String INCORRECT = "incorrect";

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