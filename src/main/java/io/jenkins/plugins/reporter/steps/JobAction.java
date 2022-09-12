package io.jenkins.plugins.reporter.steps;

import edu.hm.hafner.echarts.JacksonFacade;
import hudson.model.Action;
import hudson.model.Job;

public class JobAction implements Action {

    private final Job<?, ?> owner;

    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    
    public static final String ICON = "/plugin/nested-data-reporting/icons/data-reporting-icon.svg";

    /**
     * Creates a new instance of {@link JobAction}.
     *
     * @param owner
     *         the job that owns this action
     */
    public JobAction(final Job<?, ?> owner) {
        this.owner = owner;
    }
    
    @Override
    public String getIconFileName() {
        return ICON;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    /**
     * Returns the job this action belongs to.
     *
     * @return the job
     */
    public Job<?, ?> getOwner() {
        return owner;
    }
}
