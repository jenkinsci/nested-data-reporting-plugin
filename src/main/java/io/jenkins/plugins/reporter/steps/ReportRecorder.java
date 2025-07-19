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
import java.util.Arrays;
import java.util.Locale;

import hudson.tasks.BuildStepMonitor;

public class ReportRecorder extends Publisher {
    
    private String name;
    
    private Provider provider;
    
    private String displayType;

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
        return this;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    @DataBoundSetter
    public void setProvider(final Provider provider) {
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getDisplayType() {
        return displayType;
    }

    @DataBoundSetter
    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

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
        return record(run, workspace, listener);
    }

    private ReportResult record(final Run<?, ?> run, final FilePath workspace, final TaskListener listener) 
            throws IOException, InterruptedException {
    
        Report report = scan(run, workspace, listener, provider);
        report.setName(getName());

        DisplayType dt = Arrays.stream(DisplayType.values())
                .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(getDisplayType()))
                .findFirst().orElse(DisplayType.ABSOLUTE);
        
        report.setDisplayType(dt);
        
    return publishReport(run, listener, provider.getSymbolName(), report);
    }

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
