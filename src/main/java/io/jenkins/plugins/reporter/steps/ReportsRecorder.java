package io.jenkins.plugins.reporter.steps;

import edu.hm.hafner.util.FilteredLog;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Recorder;
import io.jenkins.plugins.util.LogHandler;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class ReportsRecorder extends Recorder {

    private String id = StringUtils.EMPTY;

    private String name = StringUtils.EMPTY;
    
    private Provider provider;

    private boolean failOnError = false;

    /**
     * Creates a new instance of {@link ReportsRecorder}.
     */
    @DataBoundConstructor
    public ReportsRecorder() {
        super();

        // empty constructor required for Stapler
    }

    @DataBoundSetter
    public void setId(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @DataBoundSetter
    public void setName(final String name) {
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


    @DataBoundSetter
    public void setFailOnError(final boolean failOnError) {
        this.failOnError = failOnError;
    }


    public boolean getFailOnError() {
        return failOnError;
    }

    /**
     * Executes the build step.
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
        AnnotatedReport report = new AnnotatedReport(provider.getId(), report);
        
        return  publishResult(run, listener, provider.getName(), provider.getName(), report);
    }

    ReportResult publishResult(final Run<?, ?> run, final TaskListener listener, final String reportName, 
                               final String loggerName, final AnnotatedReport report) {
       
        ReportsPublisher publisher = new ReportsPublisher(run, report, reportName,
                new LogHandler(listener, loggerName, new FilteredLog("")));
        
        ReportAction action = publisher.attachAction();
        
        return action.getResult();
    }
}
