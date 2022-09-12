package io.jenkins.plugins.reporter.steps;

import java.io.Serializable;

public class AnnotatedReport implements Serializable {
    
    private static final long serialVersionUID = -4071108444985380858L;

    private final Report aggregatedReport = new Report();

    /**
     * Returns the aggregated report.
     *
     * @return the aggregated report
     */
    public Report getReport() {
        return aggregatedReport;
    }
}
