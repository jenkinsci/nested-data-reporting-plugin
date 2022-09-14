package io.jenkins.plugins.reporter;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;

import java.io.IOException;
import java.util.List;

public class ReportScanner {

    private final Run<?, ?> run;

    private final FilePath workspace;
    
    private final Provider provider;

    private final TaskListener listener;

    public ReportScanner(final Run<?, ?> run, final Provider provider, final FilePath workspace, final TaskListener listener) {
        this.run = run;
        this.provider = provider;
        this.workspace = workspace;
        this.listener = listener;
    }
    
    public Report scan() throws IOException, InterruptedException {
        LogHandler logger = new LogHandler(listener, provider.getSymbolName());
        Report report = provider.scan(run, workspace, logger);
        logger.log(report);
        
        return report;
    }
}
