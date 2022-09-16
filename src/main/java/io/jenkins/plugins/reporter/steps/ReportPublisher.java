package io.jenkins.plugins.reporter.steps;

import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.LogHandler;

public class ReportPublisher {

    private final Run<?, ?> run;

    private final Report report;

    private final LogHandler logger;
    
    ReportPublisher(final Run<?, ?> run, final Report report, final LogHandler logger) {
        this.run = run;
        this.report = report;
        this.logger = logger;
    }
    
    ReportAction attachAction() {
        ReportResult result = new ReportResult(run, report);
        ReportAction action = new ReportAction(run, result, report.getName());
        run.addAction(action);
        
        return action;
    }

    private String getId() {
        return report.getId();
    }
}
