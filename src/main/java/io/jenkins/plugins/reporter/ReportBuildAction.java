package io.jenkins.plugins.reporter;

import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JobAction;
import org.kohsuke.stapler.StaplerProxy;

public class ReportBuildAction extends BuildAction<Report> implements StaplerProxy {

    private final Report report;
    
    protected ReportBuildAction(Run<?, ?> owner, Report report) {
        super(owner, report, true);
        this.report = report;
    }

    public ReportBuildAction(Run<?, ?> owner, Report report, boolean canSerialize) {
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
    public double getTotalAccurateRate() {
        return getResult().getTotalAccurate() / (double) getResult().getTotal() * 100;
    }

    @SuppressWarnings("unused")
    public double getTotalManuallyRate() {
        return getResult().getTotalManually() / (double) getResult().getTotal() * 100;
    }

    @SuppressWarnings("unused")
    public double getTotalIncorrectRate() {
        return getResult().getTotalIncorrect() / (double) getResult().getTotal() * 100;
    }
}
