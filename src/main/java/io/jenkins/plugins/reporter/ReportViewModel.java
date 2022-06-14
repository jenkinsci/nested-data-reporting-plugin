package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.reporter.charts.AssetSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public String getAssetDataModel(Item asset) {
        PieChartModel model = new PieChartModel(asset.getId());

        model.add(new PieData("Accurate", 2), Palette.GREEN);
        model.add(new PieData("Manually", 4), Palette.YELLOW);
        model.add(new PieData("Incorrect", 6), Palette.RED);

        return new JacksonFacade().toJson(model);
    }

    private String createTrendAsJson(final TrendChart trendChart, final String configuration, String id) {
        Job<?, ?> job = getOwner().getParent();
        RunList<?> runs = job.getBuilds();

        List<ReportAction> reports = runs.stream()
                .filter(run -> run.getNumber() <= getOwner().getNumber())
                .map(run -> run.getAction(ReportAction.class))
                .collect(Collectors.toList());

        List<BuildResult<ReportAction>> history = new ArrayList<>();
        for (ReportAction report : reports) {
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
        return new ArrayList<String>(Collections.singleton("Aktien"));
    }
    
}
