package io.jenkins.plugins.reporter;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.model.ColorPalette;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.util.LogHandler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

        if (!report.hasColors()) {
            report.logInfo("Report has no colors! Try to find the colors of the previous report.");
            
            Optional<Report> prevReport = findPreviousReport(run, report.getId());

            if (prevReport.isPresent()) {
                Report previous = prevReport.get();

                if (previous.hasColors()) {
                    report.logInfo("Previous report has colors. Add it to this report.");
                    report.setColors(previous.getColors());
                } else {
                    report.logInfo("Previous report has no colors. Will generate color palette.");
                    report.setColors(new ColorPalette(report.getColorIds()).generatePalette());
                }

            } else {
                report.logInfo("No previous report found. Will generate color palette.");
                report.setColors(new ColorPalette(report.getColorIds()).generatePalette());
            }
        }
        
        logger.log(report);
        
        return report;
    }

    public Optional<Report> findPreviousReport(Run<?,?> run, String id) {
        Run<?, ?> prevBuild = run.getPreviousBuild();

        if (prevBuild != null) {
            List<ReportAction> prevReportActions = prevBuild.getActions(ReportAction.class);
            Optional<ReportAction> prevReportAction = prevReportActions.stream()
                    .filter(reportAction -> Objects.equals(reportAction.getResult().getReport().getId(), id))
                    .findFirst();

            return prevReportAction
                    .map(reportAction -> Optional.of(reportAction.getResult().getReport()))
                    .orElseGet(() -> findPreviousReport(prevBuild, id));
        }

        return Optional.empty();
    }
}
