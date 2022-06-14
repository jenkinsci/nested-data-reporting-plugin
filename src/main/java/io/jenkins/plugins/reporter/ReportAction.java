package io.jenkins.plugins.reporter;

import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JobAction;
import org.kohsuke.stapler.StaplerProxy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportAction extends BuildAction<Report> implements StaplerProxy {

    private final Report report;
    
    protected ReportAction(Run<?, ?> owner, Report report) {
        super(owner, report, true);
        this.report = report;
    }

    public ReportAction(Run<?, ?> owner, Report report, boolean canSerialize) {
        super(owner, report, canSerialize);
        this.report = report;
    }

    public Report getReport() {
        return report;
    }

    @Override
    protected ReportXmlStream createXmlStream() {
        return new ReportXmlStream();
    }

    @Override
    protected JobAction<? extends BuildAction<Report>> createProjectAction() {
        return new ReportJobAction(getOwner().getParent());
    }

    @Override
    protected String getBuildResultBaseName() {
        return "data-report.xml";
    }

    @Override
    public String getIconFileName() {
        return ReportJobAction.BIG_ICON;
    }

    @Override
    public String getDisplayName() {
        return report.getLabel();
    }

    @Override
    public String getUrlName() {
        return ReportJobAction.ID;
    }

    @Override
    public Object getTarget() {
        return new ReportViewModel(getOwner(), getResult());
    }

    @SuppressWarnings("unused")
    public Map<String, Double> getAggregatedRelativeReportItemResults() {
        int total = getResult().aggregate().values().stream().reduce(0, Integer::sum);
        return getResult().aggregate().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                item -> BigDecimal.valueOf((double) item.getValue() * 100 / total)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue()));
    }
}
