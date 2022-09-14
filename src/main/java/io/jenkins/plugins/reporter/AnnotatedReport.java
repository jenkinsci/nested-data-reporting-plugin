package io.jenkins.plugins.reporter;

import io.jenkins.plugins.reporter.model.Report;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.List;

public class AnnotatedReport implements Serializable {
    
    private static final long serialVersionUID = -4071108444985380858L;

    private final String id;
    
    private final String providerName;
    
    private final Report aggregatedReport = new Report();

    /**
     * Creates a new instance of {@link AnnotatedReport}.
     *
     * @param id
     *         the ID of the report
     */
    public AnnotatedReport(final String id, final String providerName) {
        this.id = id;
        this.providerName = providerName;
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

    public void add(Report report) {
        aggregatedReport.add(report);
        System.out.println(String.format("Add Report with ID='%s' to AnnotatedReport with ID='%s'",
                report.getId(), getId()));
    }
    
}
