package io.jenkins.plugins.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
import io.jenkins.cli.shaded.org.apache.commons.io.FilenameUtils;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.Result;
import jenkins.tasks.SimpleBuildStep;
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
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Locale;

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
    
    private String reportFile;
    
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

    public String getReportFile() {
        return reportFile;
    }

    @DataBoundSetter
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
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace, @NonNull EnvVars env, 
                        @NonNull Launcher launcher, @NonNull TaskListener listener) throws InterruptedException, 
                        IOException {
        listener.getLogger().println("[PublishReportStep] Report data... ");
        
        FilePath filePath = workspace.child(getReportFile());
        String extension =  FilenameUtils.getExtension(filePath.getName()).toLowerCase(Locale.ROOT);
        
        String json;

        switch (extension) {
            case "yaml":
            case "yml": {
                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                Object obj = yamlReader.readValue(filePath.readToString(), Object.class);
                ObjectMapper jsonWriter = new ObjectMapper();
                json = jsonWriter.writeValueAsString(obj);
                break;
            }
            case "json":
                json = filePath.readToString();
                break;
            default:
                throw new InvalidObjectException("File extension is not supported!");
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

            DisplayType dt = Arrays.stream(DisplayType.values())
                    .filter(e -> e.name().toLowerCase(Locale.ROOT).equals(getDisplayType()))
                    .findFirst().orElse(DisplayType.ABSOLUTE);
                    
            Report report = new Report(result, dt);
            run.addAction(new ReportAction(run, report));

            listener.getLogger().println(String.format("[PublishReportStep] Add report with display type %s to current build.", dt.name()));
            
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