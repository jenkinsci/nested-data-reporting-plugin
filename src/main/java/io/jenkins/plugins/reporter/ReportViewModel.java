package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.reporter.charts.AssetSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Asset;
import io.jenkins.plugins.reporter.model.Report;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReportViewModel implements ModelObject {
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    
    private final Run<?, ?> owner;
    private final Report report;

    /**
     * Creates a new instance of {@link ReportViewModel}.
     *
     * @param owner
     *         the build as owner of this view
     * @param report
     *         the report to show in the view
     */
    ReportViewModel(final Run<?, ?> owner, final Report report) {
        super();

        this.owner = owner;
        this.report = report;
    }
    
    public Run<?, ?> getOwner() {
        return owner;
    }
    
    @Override
    public String getDisplayName() {
        return report.getLabel();
    }

    public Report getReport() {
        return report;
    }
    
    /**
     * Returns the UI model for an ECharts report data chart.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getAssetDataModel(Asset asset) {
        PieChartModel model = new PieChartModel(asset.getId());

        model.add(new PieData("Accurate", asset.getAccurate()), Palette.GREEN);
        model.add(new PieData("Manually", asset.getManually()), Palette.YELLOW);
        model.add(new PieData("Incorrect", asset.getIncorrect()), Palette.RED);

        return new JacksonFacade().toJson(model);
    }

    private String createTrendAsJson(final TrendChart trendChart, final String configuration, String id) {
        Job<?, ?> job = getOwner().getParent();
        RunList<?> runs = job.getBuilds();

        List<ReportBuildAction> reports = runs.stream()
                .filter(run -> run.getNumber() <= getOwner().getNumber())
                .map(run -> run.getAction(ReportBuildAction.class))
                .collect(Collectors.toList());

        List<BuildResult<ReportBuildAction>> history = new ArrayList<>();
        for (ReportBuildAction report : reports) {
            Build build = new Build(report.getOwner().getNumber(), report.getOwner().getDisplayName(), 0);
            history.add(new BuildResult<>(build, report));
        }

        AssetSeriesBuilder builder = new AssetSeriesBuilder(id);
        return new JacksonFacade().toJson(trendChart.create(history, ChartModelConfiguration.fromJson(configuration), builder));
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the asset properties.
     *
     * @param configuration
     *         determines whether the Jenkins build number should be used on the X-axis or the date
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public String getBuildTrend(final String configuration, String id) {
        return createTrendAsJson(new TrendChart(), configuration, id);
    }

    /**
     * Returns the ids of the assets to render the trend charts.
     *
     * @return the ids of assets as list
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public List<String> getAssetIds() {
        return getReport().getAssets().stream().map(Asset::getId).collect(Collectors.toList());
    }
    
}
