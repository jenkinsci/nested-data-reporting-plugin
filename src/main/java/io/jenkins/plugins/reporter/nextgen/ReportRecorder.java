package io.jenkins.plugins.reporter.nextgen;

import edu.hm.hafner.util.FilteredLog;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Recorder;
import io.jenkins.plugins.util.LogHandler;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;

public class ReportRecorder extends Recorder {
    
    private String name;
    
    private Provider provider;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @DataBoundSetter
    public void setProvider(final Provider provider) {
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
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
        AnnotatedReport report = new AnnotatedReport(provider.getSymbolName());
        report.add(scan(run, workspace, listener, provider));
        return publishResult(run, listener, getName(), provider.getSymbolName(), report);
    }

    ReportResult publishResult(final Run<?, ?> run, final TaskListener listener, final String reportName, 
                               final String loggerName, final AnnotatedReport report) {
       
        ReportPublisher publisher = new ReportPublisher(run, report, reportName,
                new LogHandler(listener, loggerName, new FilteredLog("ReportsPublisher")));
        
        ReportAction action = publisher.attachAction();
        
        return action.getResult();
    }

    private AnnotatedReport scan(final Run<?, ?> run, final FilePath workspace, final TaskListener listener,
                                         final Provider provider) throws IOException, InterruptedException {
        
        ReportScanner reportScanner = new ReportScanner(run, provider, workspace, listener);
        return reportScanner.scan();
    }
}
