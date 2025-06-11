package io.jenkins.plugins.reporter.steps;

import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;
import io.jenkins.plugins.reporter.ReportScanner;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.JenkinsFacade;
import io.jenkins.plugins.util.LogHandler;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ReportRecorder extends Recorder {

    public static class ReportConfig {
        private String name;
        private Provider provider;
        private String displayType;

        @DataBoundConstructor
        public ReportConfig(String name, Provider provider, String displayType) {
            this.name = name;
            this.provider = provider;
            this.displayType = displayType;
        }

        public String getName() {
            return name;
        }

        @DataBoundSetter
        public void setName(String name) {
            this.name = name;
        }

        public Provider getProvider() {
            return provider;
        }

        @DataBoundSetter
        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public String getDisplayType() {
            return displayType;
        }

        @DataBoundSetter
        public void setDisplayType(String displayType) {
            this.displayType = displayType;
        }
    }
    
    private List<ReportConfig> reportConfigs = new ArrayList<>();

    // TODO: remove old fields later after ensuring readResolve handles them
    private transient String name;
    private transient Provider provider;
    private transient String displayType;

    /**
     * Creates a new instance of {@link ReportRecorder}.
     */
    @DataBoundConstructor
    public ReportRecorder() {
        super();

        // empty constructor required for Stapler
    }

    /**
     * Called after de-serialization to retain backward compatibility or to populate new elements (that would be
     * otherwise initialized to {@code null}).
     *
     * @return this
     */
    protected Object readResolve() {
        if (reportConfigs == null || reportConfigs.isEmpty()) {
            if (name != null || provider != null || displayType != null) {
                reportConfigs = new ArrayList<>();
                reportConfigs.add(new ReportConfig(name, provider, displayType));
            }
        }
        // Ensure old fields are null after migration to prevent them from being persisted if not transient
        // However, since they are marked transient, this is mostly for logical clarity during readResolve
        this.name = null;
        this.provider = null;
        this.displayType = null;

        return this;
    }

    public List<ReportConfig> getReportConfigs() {
        return reportConfigs;
    }

    @DataBoundSetter
    public void setReportConfigs(List<ReportConfig> reportConfigs) {
        this.reportConfigs = reportConfigs;
    }

    // Old setters and getters are removed as fields are now transient
    // and new interactions go through reportConfigs list.

    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {
        FilePath workspace = build.getWorkspace();
        if (workspace == null) {
            throw new IOException("No workspace found for " + build);
        }

        perform(build, workspace, listener);

        return true;
    }
    
    /**
     * Executes the build step.jelly.
     *
     * @param run
     *         the run of the pipeline or freestyle job
     * @param workspace
     *         workspace of the build
     * @param listener
     *         the logger
     *
     * @return the created results
     */
    ReportResult perform(final Run<?, ?> run, final FilePath workspace, final TaskListener listener)
            throws InterruptedException, IOException {
        // For now, we'll return the result of the last processed report,
        // or a new ReportResult if the list is empty.
        // Consider a more sophisticated way to aggregate results or handle errors.
        ReportResult finalResult = new ReportResult();
        if (reportConfigs == null || reportConfigs.isEmpty()) {
            listener.getLogger().println("[Reporter] No report configurations provided.");
            return finalResult; // Return an empty result or handle as an error
        }

        for (ReportConfig config : reportConfigs) {
            try {
                Report report = scan(run, workspace, listener, config.getProvider());
                report.setName(config.getName());

                DisplayType dt = Arrays.stream(DisplayType.values())
                        .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(config.getDisplayType()))
                        .findFirst().orElse(DisplayType.ABSOLUTE);
                report.setDisplayType(dt);

                // It's important that publishReport can be called multiple times if needed,
                // or that ReportPublisher is instantiated correctly for each report.
                // Assuming ReportPublisher is stateless or its state is properly managed per call.
                finalResult = publishReport(run, listener, config.getProvider().getSymbolName(), report);
                listener.getLogger().println("[Reporter] Successfully processed report: " + config.getName());
            } catch (Exception e) {
                listener.error("[Reporter] Failed to process report: " + config.getName() + ". Error: " + e.getMessage());
                // Decide whether to continue with other reports or stop.
                // For now, we log the error and continue.
                // Optionally, aggregate errors or mark the build as unstable.
            }
        }
        return finalResult; // Or an aggregated result
    }

    // record method is effectively merged into the perform method's loop.
    // private ReportResult record(final Run<?, ?> run, final FilePath workspace, final TaskListener listener)
    // throws IOException, InterruptedException {
    // }

    ReportResult publishReport(final Run<?, ?> run, final TaskListener listener,
                               final String loggerName, final Report report) {

        ReportPublisher publisher = new ReportPublisher(run, report,
                new LogHandler(listener, loggerName, new FilteredLog("ReportsPublisher")));

        ReportAction action = publisher.attachAction();

        return action.getResult();
    }

    private Report scan(final Run<?, ?> run, final FilePath workspace, final TaskListener listener,
                              final Provider provider) throws IOException, InterruptedException {

        ReportScanner reportScanner = new ReportScanner(run, provider, workspace, listener);

        return reportScanner.scan();
    }

    /**
     * Descriptor for this step.jelly: defines the context and the UI elements.
     */
    @Extension
    @Symbol("publishReport")
    @SuppressWarnings("unused") // most methods are used by the corresponding jelly view
    public static class Descriptor extends BuildStepDescriptor<Publisher> {

        private static final JenkinsFacade JENKINS = new JenkinsFacade();
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Step_Name();
        }

        // called by jelly view
        @POST
        public FormValidation doCheckName(@QueryParameter("name") String name) {
            if (StringUtils.isEmpty(name)) {
                return FormValidation.error("Field 'name' is required.");
            }

            return FormValidation.ok();
        }

        // called by jelly view
        @POST
        public ListBoxModel doFillDisplayTypeItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel r = new ListBoxModel();
                for (DisplayType dt : DisplayType.values()) {
                    r.add(dt.name().toLowerCase());
                }
                return r;
            }

            return new ListBoxModel();
        }
    }
}
