package io.jenkins.plugins.reporter.nextgen;

import edu.hm.hafner.echarts.JacksonFacade;
import hudson.model.Action;
import hudson.model.Job;

public class JobAction implements Action {

    private final Job<?, ?> owner;
    
    private final String name;
    
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
    public JobAction(final Job<?, ?> owner, String name) {
        this.owner = owner;
        this.name = name;
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
        return "report-" + name.hashCode();
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
