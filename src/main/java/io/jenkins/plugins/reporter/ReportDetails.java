package io.jenkins.plugins.reporter;

import hudson.model.ModelObject;
import hudson.model.Run;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReportDetails implements ModelObject {

    private transient Run<?, ?> owner;

    private final String displayName;

    private final ReportResult result;
    
    private final String url;

    private final List<String> errorMessages = new ArrayList<>();
    
    private final List<String> infoMessages = new ArrayList<>();
    
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

        infoMessages.addAll(result.getInfoMessages().castToList());
        errorMessages.addAll(result.getErrorMessages().castToList());
    }

    /**
     * Returns the error messages of the static analysis run.
     *
     * @return the error messages
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Collection<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * Returns the information messages of the static analysis run.
     *
     * @return the information messages
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Collection<String> getInfoMessages() {
        return infoMessages;
    }

    
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
    public final Run<?, ?> getOwner() {
        return owner;
    }
    
}
