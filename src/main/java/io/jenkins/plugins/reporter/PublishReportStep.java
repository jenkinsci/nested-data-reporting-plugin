package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.JacksonFacade;
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
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.Result;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.jenkinsci.Symbol;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Publishes a report: Stores the created report in an {@link ReportAction}. The result is attached to the {@link Run}
 * by registering a {@link ReportAction}.
 *
 * @author Simon Symhoven.
 */
@Extension
public class PublishReportStep extends Builder implements SimpleBuildStep, Serializable {
    
    private String jsonString;
    private String jsonFile;
    
    @DataBoundConstructor
    public PublishReportStep() {
        super();
    }
    
    public String getJsonString() {
        return jsonString;
    }
    
    @DataBoundSetter
    public void setJsonString(final String jsonString) {
        this.jsonString = jsonString;
    }

    public String getJsonFile() {
        return jsonFile;
    }

    @DataBoundSetter
    public void setJsonFile(final String jsonFile) {
        this.jsonFile = jsonFile;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[PublishReportStep] Report data... ");
        
        String json;
        
        if (StringUtils.isNotBlank(getJsonFile())) {
            FilePath jsonFile = workspace.child(getJsonFile());
            json = jsonFile.readToString();
        } else {
            json = getJsonString();
        }
        
        try (InputStream inputStream = getClass().getResourceAsStream("/report.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            SchemaLoader schemaLoader = SchemaLoader.builder()
                    .schemaClient(SchemaClient.classPathAwareClient())
                    .schemaJson(rawSchema)
                    .resolutionScope("classpath:/")
                    .build();
            Schema schema = schemaLoader.load().build();
            schema.validate(new JSONObject(json));
            
            JacksonFacade jackson = new JacksonFacade();
            Result result =  jackson.fromJson(json, Result.class);
            Report report = new Report(result);
            run.addAction(new ReportAction(run, report));

            listener.getLogger().println("[PublishReportStep] Add report to current build.");
            
        } catch (ValidationException e) {
            listener.getLogger().printf("[PublishReportStep] error: %s", e.getMessage());
        }
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