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
import io.jenkins.plugins.reporter.model.Asset;
import io.jenkins.plugins.reporter.model.Report;
import jenkins.tasks.SimpleBuildStep;
import net.sf.jsefa.Deserializer;
import net.sf.jsefa.common.lowlevel.filter.HeaderAndFooterFilter;
import net.sf.jsefa.csv.CsvIOFactory;
import net.sf.jsefa.csv.config.CsvConfiguration;
import org.apache.commons.compress.utils.Lists;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.*;
import java.util.List;

@Extension
public class ReportBuildStep extends Builder implements SimpleBuildStep, Serializable {
    
    private String csv;

    private String label;
    
    @DataBoundConstructor
    public ReportBuildStep() {
        super();
        this.label = "Data Reporting";
    }
    
    public String getCsv() {
        return csv;
    }
    
    @DataBoundSetter
    public void setCsv(String csv) {
        this.csv = csv;
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
        listener.getLogger().println("Report data... ");
        listener.getLogger().println("with csv: " + getCsv());
        listener.getLogger().println("and label: " + getLabel());

        CsvConfiguration config = new CsvConfiguration();
        config.setLineFilter(new HeaderAndFooterFilter(1, false, true));
        
        Deserializer deserializer = CsvIOFactory.createFactory(config, Asset.class).createDeserializer();
        
        File csvFile = new File(workspace.toURI().getPath(), csv);
        InputStream csvStream = new FileInputStream(csvFile);
        deserializer.open(new InputStreamReader(csvStream,  "UTF-8"));
            
        Report report = new Report();
        report.setCsv(getCsv());
        report.setLabel(getLabel());
        List<Asset> assets = Lists.newArrayList();
        while (deserializer.hasNext()) {
            Asset asset = deserializer.next();
            assets.add(asset);
        }
        deserializer.close(true);
        report.setAssets(assets);
        
        run.addAction(new ReportBuildAction(run, report));
    }
 
    @Extension 
    @Symbol("reportData")
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