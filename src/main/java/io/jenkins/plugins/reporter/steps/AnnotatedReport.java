package io.jenkins.plugins.reporter.steps;

import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;

public class AnnotatedReport implements Serializable {
    
    private static final long serialVersionUID = -4071108444985380858L;

    private final String id;
    
    private final Report aggregatedReport = new Report();

    public AnnotatedReport(final String id, Report report) {
        this.id = id;
        addReport(report);
    }
   
    /**
     * Returns the ID of this report.
     *
     * @return the ID
     */
    @Whitelisted
    public String getId() {
        return id;
    }
    
    /**
     * Returns the aggregated report.
     *
     * @return the aggregated report
     */
    public Report getReport() {
        return aggregatedReport;
    }

    private void addReport(Report report) {
        // TODO
    }


}
