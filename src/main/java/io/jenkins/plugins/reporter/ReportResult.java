package io.jenkins.plugins.reporter;

import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Report;
import java.util.Collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ReportResult implements Serializable {

    private static final long serialVersionUID = 7761451736733548294L;

    private transient Run<?, ?> owner;

    private final Report report;
    
    private final List<String> errors;
    
    private final List<String> messages;
    
    public ReportResult(final Run<?, ?> owner, final Report report) {
        this.owner = owner;
        this.report = report;
        messages = new ArrayList<>(report.getInfoMessages());
        errors = new ArrayList<>(report.getErrorMessages());
    }
    
    public Run<?, ?> getOwner() {
        return owner;
    }

    /**
     * Sets the run for this result after Jenkins read its data from disk.
     *
     * @param owner
     *         the initialized run
     */
    public void setOwner(final Run<?, ?> owner) {
        this.owner = owner;
    }

    public Report getReport() {
        return report;
    }
    
    public List<String> getErrorMessages() {
        return Collections.unmodifiableList(errors);
    }
    
    public List<String> getInfoMessages() {
        return Collections.unmodifiableList(messages);
    }
}
