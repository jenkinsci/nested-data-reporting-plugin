package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.LinesChartModel;
import hudson.model.Job;
import hudson.util.RunList;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.ReportTrendChart;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Report;

import java.util.*;
import java.util.stream.Collectors;

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
    
    /**
     * Creates a new instance of {@link ReportJobAction}.
     *
     * @param owner
     *         the job that owns this action
     */
    public ReportJobAction(final Job<?, ?> owner, Report report) {
        super(owner, ReportAction.class);
        this.report = report;
    }
    
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @Override
    public String getDisplayName() {
        return report.getResult().getName();
    }

    @Override
    public String getUrlName() {
        return ID + "-" + report.hashCode();
    }

    @Override
    protected LinesChartModel createChartModel(String configuration) {
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);

        RunList<?> runs = getOwner().getBuilds();

        List<ReportAction> reports = runs.stream()
                .map(run -> Optional.of(run.getActions(ReportAction.class)))
                .map(Optional::get)
                .flatMap(List::stream)
                .filter(reportAction -> Objects.equals(reportAction.getReport().getResult().getId(), this.report.getResult().getId()))
                .collect(Collectors.toList());
        
        List<BuildResult<ReportAction>> history = reports.stream()
                .map(reportAction -> new BuildResult<>(new Build(reportAction.getOwner().getNumber(), 
                        reportAction.getOwner().getDisplayName(), 0), reportAction))
                .collect(Collectors.toList());
        
        return new ReportTrendChart().create(history, modelConfiguration, report, report.getResult().getItems());
        

    }
}
