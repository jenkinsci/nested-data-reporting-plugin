package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import hudson.model.Job;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;

public class ReportJobAction extends AsyncConfigurableTrendJobAction<ReportBuildAction> {
    
    static final String SMALL_ICON = "/plugin/data-reporting/icons/wm-logo-24x24.png";
    static final String BIG_ICON = "/plugin/data-reporting/icons/wm-logo-48x48.png";
    static final String ID = "data-reporting";

    /**
     * Creates a new instance of {@link ReportJobAction}.
     *
     * @param owner
     *         the job that owns this action
     */
    public ReportJobAction(final Job<?, ?> owner) {
        super(owner, ReportBuildAction.class);
    }
    
    @Override
    public String getIconFileName() {
        return BIG_ICON;
    }

    @Override
    public String getDisplayName() {
        return Messages.Action_Name();
    }

    @Override
    public String getUrlName() {
        return ID;
    }

    @Override
    protected LinesChartModel createChartModel(String configuration) {
        return createChart(getBuildHistory(), configuration);
    }
    
    public Iterable<? extends BuildResult<ReportBuildAction>> getBuildHistory() {
        return createBuildHistory();
    }

    LinesChartModel createChart(final Iterable<? extends BuildResult<ReportBuildAction>> buildHistory,
                                final String configuration) {
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);
        return new TrendChart().create(buildHistory, modelConfiguration, new ReportSeriesBuilder());
    }
}
