package io.jenkins.plugins.reporter.steps;

import hudson.model.ModelObject;
import hudson.model.Run;

public class ReportDetails implements ModelObject {

    private transient Run<?, ?> owner;

    private final String displayName;

    private final ReportResult result;
    
    private final String url;
    
    /**
     * Creates a new instance of {@link ReportDetails}.
     *
     * @param owner
     *          the associated build/run of this view
     * @param url
     *          the relative URL of this view
     * @param result
     *          the report result
     * @param displayName
     *          the human-readable name of this view (shown in breadcrumb).
     */
    public ReportDetails(final Run<?, ?> owner, final String url, final ReportResult result, final String displayName) {
        super();

        this.owner = owner;
        this.url = url;
        this.result = result;
        this.displayName = displayName;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
}
