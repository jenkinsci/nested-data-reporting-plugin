package io.jenkins.plugins.reporter;

import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.BuildAction;
import io.jenkins.plugins.util.JobAction;
import org.kohsuke.stapler.StaplerProxy;

/**
 * Controls the life cycle of the data report a job. This action persists the results of a data report.
 * 
 * This action also provides access to the report details: these are rendered using a new {@link ItemViewModel} 
 * instance.
 *
 * @author Simon Symhoven
 */
public class ReportAction extends BuildAction<Report> implements StaplerProxy {

    private final Report report;
    public final static String REPORT_ID = "report";
    private final String label;

    /**
     * Creates a new instance of {@link ReportAction}.
     *
     * @param owner
     *         the associated build/run that created the static analysis result
     * @param report
     *         the report to add to the action.
     */
    protected ReportAction(Run<?, ?> owner, Report report, String label) {
        this(owner, report, label, true);
    }

    /**
     * Creates a new instance of {@link ReportAction}.
     *
     * @param owner
     *         the associated build/run that created the static analysis result
     * @param report
     *         the report to add to the action
     * @param canSerialize
     *         if the action can be serialized.
     */
    public ReportAction(Run<?, ?> owner, Report report, String label, boolean canSerialize) {
        super(owner, report, canSerialize);
        this.report = report;
        this.label = label;
    }

    /**
     * Returns the {@link Report} controlled by this action.
     * 
     * @return the report.
     */
    public Report getReport() {
        return report;
    }

    @Override
    protected ReportXmlStream createXmlStream() {
        return new ReportXmlStream();
    }

    @Override
    protected JobAction<? extends BuildAction<Report>> createProjectAction() {
        return new ReportJobAction(getOwner().getParent(), getReport(), label);
    }

    @Override
    protected String getBuildResultBaseName() {
        return "nested-data-report.xml";
    }

    @Override
    public String getIconFileName() {
        return ReportJobAction.ICON;
    }

    @Override
    public String getDisplayName() {
        return label;
    }

    @Override
    public String getUrlName() {
        return ReportJobAction.ID + "-" + this.getReport().hashCode();
    }

    /**
     * Returns the detail view for items for all Stapler requests.
     *
     * @return the detail view for items
     */
    @Override
    public ItemViewModel getTarget() {
        Item item = new Item();
        item.setId(REPORT_ID);
        item.setName(Messages.Module_Name());
        item.setResult(report.getResult().aggregate());
        item.setItems(report.getResult().getItems());
        
        return new ItemViewModel(getOwner(), ReportJobAction.ID, item, item.getName(), null , getReport());
    }
}
