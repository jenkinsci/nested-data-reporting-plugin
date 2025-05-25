package io.jenkins.plugins.reporter;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.model.ColorPalette;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.util.LogHandler;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public Report processDiffReport(Report currentReport, Run<?, ?> currentRun, TaskListener listener) {
        Optional<Report> previousSuccessfulReportOptional = findPreviousSuccessfulReport(currentRun.getPreviousSuccessfulBuild(), currentReport.getId(), listener);
        if (previousSuccessfulReportOptional.isPresent()) {
            Report previousSuccessfulReport = previousSuccessfulReportOptional.get();
            listener.getLogger().println("Previous successful report found: " + previousSuccessfulReport.getId() + ". Calculating diff.");

            Map<String, Item> previousItemsMap = previousSuccessfulReport.getItems().stream()
                    .collect(Collectors.toMap(Item::getId, Function.identity()));

            for (Item currentItem : currentReport.getItems()) {
                processItemDiff(currentItem, previousItemsMap.get(currentItem.getId()), listener, previousItemsMap);
            }
        } else {
            listener.getLogger().println("No previous successful report found. Current values will be displayed as is.");
        }
        return currentReport;
    }

    private void processItemDiff(Item currentItem, @Nullable Item previousItem, TaskListener listener, Map<String, Item> previousItemsMap) {
        if (previousItem == null) {
            listener.getLogger().println(String.format("Item with ID '%s' (name: '%s') not found in previous successful report. Current values will be used as diff.", currentItem.getId(), currentItem.getName()));
            // If previousItem is null, all current values are considered "new".
            // No change to currentItem.getResult() needed, as its values are already the diff against a non-existent previous item.
            // We still need to process its children, if any, against a null previousItemContainer for them.
        } else {
            LinkedHashMap<String, Integer> diffResult = new LinkedHashMap<>();
            // Process current item's keys
            for (Map.Entry<String, Integer> entry : currentItem.getResult().entrySet()) {
                String key = entry.getKey();
                int currentValue = entry.getValue();
                int previousValue = previousItem.getResult().getOrDefault(key, 0);
                diffResult.put(key, currentValue - previousValue);
            }

            // Process keys in previous item but not in current item (removed values)
            for (Map.Entry<String, Integer> entry : previousItem.getResult().entrySet()) {
                String key = entry.getKey();
                if (!currentItem.getResult().containsKey(key)) {
                    int previousValue = entry.getValue();
                    diffResult.put(key, 0 - previousValue);
                }
            }
            currentItem.setResult(diffResult);
        }

        // Recursive call for children, regardless of whether previousItem was null,
        // as children of a "new" item are also "new".
        if (currentItem.hasItems()) {
            Map<String, Item> childPreviousItemsMap = Collections.emptyMap();
            if (previousItem != null && previousItem.hasItems()) {
                childPreviousItemsMap = previousItem.getItems().stream()
                        .collect(Collectors.toMap(Item::getId, Function.identity()));
            }
            
            for (Item childCurrentItem : currentItem.getItems()) {
                processItemDiff(childCurrentItem, childPreviousItemsMap.get(childCurrentItem.getId()), listener, childPreviousItemsMap);
            }
        }
    }
    
    private Optional<Report> findPreviousSuccessfulReport(Run<?,?> build, String id, TaskListener listener) {
        if (build == null) {
            return Optional.empty();
        }

        if (build.getResult() == Result.SUCCESS) {
            List<ReportAction> reportActions = build.getActions(ReportAction.class);
            Optional<ReportAction> reportAction = reportActions.stream()
                    .filter(action -> Objects.equals(action.getResult().getReport().getId(), id))
                    .findFirst();

            if (reportAction.isPresent()) {
                return Optional.of(reportAction.get().getResult().getReport());
            }
        }
        
        // Check previous successful build only if current was not successful or no report action found
        Run<?, ?> previousBuild = build.getPreviousSuccessfulBuild();
        if (previousBuild != null) {
             return findPreviousSuccessfulReport(previousBuild, id, listener);
        }
       
        return Optional.empty();
    }

    private Optional<Report> findPreviousReport(Run<?,?> run, String id) {
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
