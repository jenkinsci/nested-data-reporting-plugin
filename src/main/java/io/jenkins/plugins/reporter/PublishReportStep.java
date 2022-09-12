package io.jenkins.plugins.reporter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.reporter.model.*;
import io.jenkins.plugins.reporter.steps.DisplayType;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Publishes a report: Stores the created report in an {@link ReportAction}. The result is attached to the {@link Run}
 * by registering a {@link ReportAction}.
 *
 * @author Simon Symhoven.
 */
@Extension
public class PublishReportStep extends Builder implements SimpleBuildStep, Serializable {
    
    @Deprecated
    private String jsonString;
    
    @Deprecated
    private String jsonFile;
    
    private String displayType;

    @Deprecated
    private String reportFile;

    private String pattern;
    
    @DataBoundConstructor
    public PublishReportStep() {
        super();
    }
    
    @Deprecated
    public String getJsonString() {
        return jsonString;
    }

    /**
     * use {@link #setReportFile(String)} instead.
     */
    @DataBoundSetter
    @Deprecated
    public void setJsonString(final String jsonString) {
        this.jsonString = jsonString;
    }
    
    @Deprecated
    public String getJsonFile() {
        return jsonFile;
    }
    
    /**
     * use {@link #setReportFile(String)} instead.
     */
    @DataBoundSetter
    @Deprecated
    public void setJsonFile(final String jsonFile) {
        this.jsonFile = jsonFile;
    }
    
    @Deprecated
    public String getReportFile() {
        return reportFile;
    }

    /**
     * use {@link #setPattern(String)} instead.
     */
    @DataBoundSetter
    @Deprecated
    public void setReportFile(final String reportFile) {
        this.reportFile = reportFile;
    }
    
    public String getDisplayType() {
        return displayType;
    }
    
    @DataBoundSetter
    public void setDisplayType(final String displayType) {
        this.displayType = displayType;
    }

    public String getPattern() {
        return pattern;
    }

    @DataBoundSetter
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, 
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, 
                        IOException {
        
        listener.getLogger().println("[PublishReportStep] Report data... ");
        
        List<Result> results = workspace.act(new FilesScanner(getPattern()));
        
        List<Result> resultsWithoutColor = results.stream()
                .filter(result -> !result.hasColors())
                .collect(Collectors.toList());
        
        for (Result result : resultsWithoutColor) {
            
            Optional<Report> prevReport = findPreviousReport(run, result.getId());

            List<String> colorIds = new ArrayList<>(result.getColorIds());
            ColorPalette palette = new ColorPalette(colorIds);
            
            if (prevReport.isPresent()) {
                Report report = prevReport.get();
                
                if (report.getResult().hasColors()) {
                    result.setColors(report.getResult().getColors());
                } else {
                    result.setColors(palette.generatePalette());
                }
                
            } else {
                result.setColors(palette.generatePalette());
            }
        }
        
        DisplayType dt = Arrays.stream(DisplayType.values())
                .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(getDisplayType()))
                .findFirst().orElse(DisplayType.ABSOLUTE);
        
        results.stream()
                .map(result ->  new Report(result, dt))
                .forEach(report -> {
                    run.addAction(new ReportAction(run, report));
                    listener.getLogger().println(String.format("[PublishReportStep] Add report with id %s to current build.",
                            report.getResult().getId()));
                });
    }
    
    public Optional<Report> findPreviousReport(Run<?,?> run, String id) {
        Run<?, ?> prevBuild = run.getPreviousBuild();
        
        if (prevBuild != null) {
            List<ReportAction> prevReportActions = prevBuild.getActions(ReportAction.class);
            Optional<ReportAction> prevReportAction = prevReportActions.stream()
                    .filter(reportAction -> Objects.equals(reportAction.getReport().getResult().getId(), id))
                    .findFirst();

            return prevReportAction
                    .map(reportAction -> Optional.of(reportAction.getReport()))
                    .orElseGet(() -> findPreviousReport(prevBuild, id));
        } 
        
        return Optional.empty();
    }
 
    @Extension 
    @Symbol("publishReport")
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Step_Name();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}