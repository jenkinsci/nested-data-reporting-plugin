package io.jenkins.plugins.reporter.nextgen;

import hudson.model.Run;

import java.io.Serializable;

public class ReportResult implements Serializable {

    private static final long serialVersionUID = 7761451736733548294L;

    private transient Run<?, ?> owner;
    
    public ReportResult(final Run<?, ?> owner) {
        this.owner = owner;
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
}
