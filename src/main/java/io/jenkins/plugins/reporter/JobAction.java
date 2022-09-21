package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.JacksonFacade;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.Run;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendChart;
import io.jenkins.plugins.reporter.charts.ItemHistoryChart;
import io.jenkins.plugins.reporter.model.*;
import io.jenkins.plugins.reporter.util.BuildResultNavigator;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class JobAction implements AsyncConfigurableTrendChart, Action {

    private final Job<?, ?> owner;
    
    private final String name;
    
    private final Report report;
    
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    
    public static final String ICON = "/plugin/nested-data-reporting/icons/data-reporting-icon.svg";

    /**
     * Creates a new instance of {@link JobAction}.
     *
     * @param owner
     *         the job that owns this action
     * @param name 
     *          the human-readable name
     */
    public JobAction(final Job<?, ?> owner, String name, final Report report) {
        this.owner = owner;
        this.name = name;
        this.report = report;
    }
    
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getUrlName() {
        try {
            History history = createBuildHistory();
            for (BuildResult<ReportResult> buildResult : history) {
                return buildResult.getBuild().getNumber() + "/report-" + URLEncoder.encode(name, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            return String.valueOf(getOwner().getLastBuild().getNumber());
        }

        return String.valueOf(getOwner().getLastBuild().getNumber());
    }
    
    /**
     * Returns the build history for this job.
     *
     * @return the history
     */
    public History createBuildHistory() {
        Run<?, ?> lastCompletedBuild = owner.getLastCompletedBuild();
        if (lastCompletedBuild == null) {
            return new NullReportHistory();
        }
        else {
            return new ReportHistory(lastCompletedBuild, new ByIdResultSelector(report.getId()));
        }
    }
    
    public Report getReport() {
        return report;
    }
    
    @JavaScriptMethod
    public String getTrendId() {
        return report.getId();
    }

    /**
     * Returns the job this action belongs to.
     *
     * @return the job
     */
    public Job<?, ?> getOwner() {
        return owner;
    }

    @Override
    @JavaScriptMethod
    public String getConfigurableBuildTrendModel(String configuration) {
        ChartModelConfiguration modelConfiguration = ChartModelConfiguration.fromJson(configuration);
        
        return new JacksonFacade().toJson(new ItemHistoryChart().create(createBuildHistory(), modelConfiguration, 
                new ReportSeriesBuilder(), report, report.getItems()));
    }

    @Override
    public boolean isTrendVisible() {
        return createBuildHistory().hasMultipleResults();
    }
}
