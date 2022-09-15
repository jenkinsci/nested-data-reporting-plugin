package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.model.Action;
import hudson.model.Job;
import hudson.util.RunList;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendChart;
import io.jenkins.plugins.echarts.AsyncConfigurableTrendJobAction;
import io.jenkins.plugins.reporter.charts.ItemHistoryChart;
import io.jenkins.plugins.reporter.model.ItemSeriesBuilder;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.ReportSeriesBuilder;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
        // TODO: get the last build with action instead returning the last build. Need rework of createHistory().
        try {
            return getOwner().getLastBuild().getNumber() + "/report-" + URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return String.valueOf(getOwner().getLastBuild().getNumber());
        }
    }

    public Report getReport() {
        return report;
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

        RunList<?> runs = getOwner().getBuilds();

        List<ReportAction> reports = runs.stream()
                .map(run -> Optional.of(run.getActions(ReportAction.class)))
                .map(Optional::get)
                .flatMap(List::stream)
                .filter(reportAction -> Objects.equals(reportAction.getResult().getReport().getId(), report.getId()))
                .collect(Collectors.toList());

        List<BuildResult<ReportAction>> history = reports.stream()
                .map(reportAction -> new BuildResult<>(new Build(reportAction.getOwner().getNumber(),
                        reportAction.getOwner().getDisplayName(), 0), reportAction))
                .collect(Collectors.toList());

        return new JacksonFacade().toJson(new ItemHistoryChart().create(history, modelConfiguration, new ReportSeriesBuilder(),
                report, report.getItems()));
    }

    @Override
    public boolean isTrendVisible() {
        return true;
    }
}
