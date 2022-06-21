package io.jenkins.plugins.reporter;

import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.io.Resources;
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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

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
    private String label;
    
    @DataBoundConstructor
    public PublishReportStep() {
        super();
        this.label = "Data Reporting";
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
    
    public String getLabel() {
        return label;
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("[PublishReportStep] Report data... ");
        listener.getLogger().println("[PublishReportStep] with label: " + getLabel());
        
        if (StringUtils.isNotBlank(getJsonFile())) {
            File jsonFile = new File(workspace.toURI().getPath(), getJsonFile());
            setJsonString(new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8));
        }
        
        try {
            String jsonSchema = new String(Files.readAllBytes(Paths.get(getClass().getResource("schema.json").getPath())));
            ProcessingReport validate = JsonSchemaFactory.byDefault().getJsonSchema(jsonSchema).validate(JsonLoader.fromString(getJsonString()));
            
            if (validate.isSuccess()) {
                JacksonFacade jackson = new JacksonFacade();
                Result result =  jackson.fromJson(getJsonString(), Result.class);
                Report report = new Report(result, getLabel());
                run.addAction(new ReportAction(run, report));
            } else {
                listener.getLogger().println("[PublishReportStep] JSON is invalid!");
            }
        } catch (ProcessingException e) {
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