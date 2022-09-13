package io.jenkins.plugins.reporter.nextgen;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.Serializable;
import java.util.List;

public class AnnotatedReport implements Serializable {
    
    private static final long serialVersionUID = -4071108444985380858L;

    private final String id;
    
    private final Report aggregatedReport = new Report();

    /**
     * Creates a new instance of {@link AnnotatedReport}.
     *
     * @param id
     *         the ID of the report
     */
    public AnnotatedReport(final String id) {
        this.id = id;
    }
    
    public AnnotatedReport(final String id, Report report) {
        this.id = id;
        addReport(report);
    }

    /**
     * Creates a new instance of {@link AnnotatedReport} as an aggregation of the specified reports.
     *
     * @param id
     *         the ID of the report
     * @param reports
     *         the reports to aggregate
     */
    public AnnotatedReport(@CheckForNull final String id, final List<Report> reports) {
        this(id);

        addAllReports(reports);
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
        System.out.printf("Add Report='%s'\n", report.getId());
    }

    public void add(final AnnotatedReport other) {
        System.out.printf("Add AnnotatedReport='%s'\n", other.getId());
    }

    private void addAllReports(List<Report> reports) {
        reports.forEach(this::addReport);
    }
    

}
