package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import hudson.model.Job;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;

/**
 * A job action displays a link on the side panel of a job. This action also is responsible to render the historical
 * trend via its associated 'floatingBox.jelly' view.
 *  
 * @author Simon Symhoven
 */
public class ReportJobAction extends AsyncConfigurableTrendJobAction<ReportAction> {
    
    static final String ICON = "/plugin/nested-data-reporting/icons/data-reporting-icon.svg";
    static final String ID = "data-reporting";

    /**
     * Creates a new instance of {@link ReportJobAction}.
     *
     * @param owner
     *         the job that owns this action
     */
    public ReportJobAction(final Job<?, ?> owner) {
        super(owner, ReportAction.class);
    }
    
    @Override
    public String getIconFileName() {
        return ICON;
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
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);
        ReportAction reportAction = getOwner().getLastCompletedBuild().getAction(ReportAction.class);
        ColorProvider colorProvider = new ColorProvider(reportAction.getReport().getResult().getColors());
        return new TrendChart().create(createBuildHistory(), modelConfiguration, new ReportSeriesBuilder(), colorProvider);
    }
}
