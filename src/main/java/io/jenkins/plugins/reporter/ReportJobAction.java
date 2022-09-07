package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import hudson.model.Job;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

/**
 * A job action displays a link on the side panel of a job. This action also is responsible to render the historical
 * trend via its associated 'floatingBox.jelly' view.
 *  
 * @author Simon Symhoven
 */
public class ReportJobAction extends AsyncConfigurableTrendJobAction<ReportAction> {
    
    static final String ICON = "/plugin/nested-data-reporting/icons/data-reporting-icon.svg";
    static final String ID = "nested-data-reporting";

    private final Report report;
    
    private String label = Messages.Action_Name();
    
    /**
     * Creates a new instance of {@link ReportJobAction}.
     *
     * @param owner
     *         the job that owns this action
     */
    public ReportJobAction(final Job<?, ?> owner, Report report, String label) {
        super(owner, ReportAction.class);
        this.report = report;
        this.label = label;
    }
    
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @Override
    public String getDisplayName() {
        return label;
    }

    @Override
    public String getUrlName() {
        return ID + "-" + report.hashCode();
    }

    @Override
    protected LinesChartModel createChartModel(String configuration) {
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);
        
        return new TrendChart().create(createBuildHistory(), modelConfiguration, new ReportSeriesBuilder(), 
                report, report.getResult().getItems());
        

    }
}
