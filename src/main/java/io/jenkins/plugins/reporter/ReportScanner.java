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
    
    private final String colorPaletteTheme;

    public ReportScanner(final Run<?, ?> run, final Provider provider, final FilePath workspace, final TaskListener listener, final String colorPaletteTheme) {
        this.run = run;
        this.provider = provider;
        this.workspace = workspace;
        this.listener = listener;
        this.colorPaletteTheme = colorPaletteTheme;
    }
    
    public Report scan() throws IOException, InterruptedException {
        LogHandler logger = new LogHandler(listener, provider.getSymbolName());
        Report report = provider.scan(run, workspace, logger);

        // Previous logic for colors removed as per new requirement to always use palette or fallback within ColorPalette itself.
        // The ColorPalette class will handle the fallback to RANDOM if themeName is null, empty, or invalid.
        
        if (report != null && report.hasItems()) {
            List<String> colorIds = report.getColorIds();
            if (colorIds != null && !colorIds.isEmpty()) {
                io.jenkins.plugins.reporter.model.ColorPalette paletteGenerator = new io.jenkins.plugins.reporter.model.ColorPalette(colorIds, this.colorPaletteTheme);
                report.setColors(paletteGenerator.generatePalette());
                report.logInfo("Applied color palette: " + (this.colorPaletteTheme != null ? this.colorPaletteTheme : "RANDOM"));
            } else {
                report.logInfo("Report has no items with IDs to assign colors or colorIds list is empty.");
            }
        } else if (report != null) {
            report.logInfo("Report is null or has no items, skipping color generation.");
        }
        // The old logic for finding previous report colors is removed.
        // The new ColorPalette will always be used.
        // If specific handling for "no colors" vs "previous colors" is still needed, it has to be re-evaluated.
        // For now, assuming new palette generation is the primary goal.

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
