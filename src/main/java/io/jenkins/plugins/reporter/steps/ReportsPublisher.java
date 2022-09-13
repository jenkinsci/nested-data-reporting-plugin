package io.jenkins.plugins.reporter.steps;

import hudson.model.Run;
import io.jenkins.plugins.util.LogHandler;

public class ReportsPublisher {

    private final Run<?, ?> run;

    private final AnnotatedReport report;

    private final String name;

    private final LogHandler logger;
    
    ReportsPublisher(final Run<?, ?> run, final AnnotatedReport report, final String name, final LogHandler logger) {
        this.run = run;
        this.report = report;
        this.name = name;
        this.logger = logger;
    }
    
    ReportAction attachAction() {
        ReportResult result = new ReportResult(run);
        ReportAction action = new ReportAction(run, result, name);
        run.addAction(action);
        
        return action;
    }

    private String getId() {
        return report.getId();
    }
}
