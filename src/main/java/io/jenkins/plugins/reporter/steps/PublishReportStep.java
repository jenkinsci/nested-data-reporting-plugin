package io.jenkins.plugins.reporter.steps;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.ReportResult;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.util.JenkinsFacade;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;
import io.jenkins.plugins.reporter.model.ColorPalette; // Added import
import java.util.List; // Added import for List

import java.io.Serializable;
import java.util.Set;

public class PublishReportStep extends Step implements Serializable {

    private static final long serialVersionUID = 423552861898621744L;
    
    private String name = StringUtils.EMPTY;
    
    private Provider provider;

    private String displayType;

    private String colorPalette;

    /**
     * Creates a new instance of {@link PublishReportStep}.
     */
    @DataBoundConstructor
    public PublishReportStep() {
        super();

        // empty constructor required for Stapler
    }

    public String getName() {
        return name;
    }
    
    @DataBoundSetter
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
    
    @DataBoundSetter
    public void setDisplayType(final String displayType) {
        this.displayType = displayType;
    }
    
    public String getDisplayType() {
        return displayType;
    }
    
    public String getColorPalette() {
        return colorPalette;
    }

    @DataBoundSetter
    public void setColorPalette(String colorPalette) {
        this.colorPalette = colorPalette;
    }
    
    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(context, this);
    }

    static class Execution extends SynchronousNonBlockingStepExecution<ReportResult> {

        private static final long serialVersionUID = -6468854519922975080L;

        private final PublishReportStep step;
        
        protected Execution(@NonNull StepContext context, final PublishReportStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected ReportResult run() throws Exception {
            ReportRecorder recorder = new ReportRecorder();
            recorder.setName(step.getName());
            recorder.setProvider(step.getProvider());
            recorder.setDisplayType(step.getDisplayType());
            recorder.setColorPalette(step.getColorPalette());
            
            return recorder.perform(getContext().get(Run.class), getContext().get(FilePath.class), 
                    getContext().get(TaskListener.class));
        }
    }
            
    /**
     * Descriptor for this step.jelly: defines the context and the UI labels.
     */
    @Extension
    public static class Descriptor extends StepDescriptor {

        private static final JenkinsFacade JENKINS = new JenkinsFacade();
        
        @Override
        public Set<Class<?>> getRequiredContext() {
            return Set.of(FlowNode.class, Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "publishReport";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Step_Name();
        }
        
        public DescriptorExtensionList<Provider, Provider.ProviderDescriptor> getProvider() {
            return Jenkins.get().getDescriptorList(Provider.class);
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
        
        // called by jelly view
        @POST
        public ListBoxModel doFillColorPaletteItems(@AncestorInPath final AbstractProject<?, ?> project) {
            if (JENKINS.hasPermission(Item.CONFIGURE, project)) {
                ListBoxModel model = new ListBoxModel();
                model.add("Default (Random Colors)", ""); // Option for default behavior
                
                List<String> themeNames = ColorPalette.getAvailableThemes();
                for (String themeName : themeNames) {
                    String originalDisplayName = StringUtils.capitalize(themeName.toLowerCase().replace('_', ' '));
                    String finalDisplayName = originalDisplayName;

                    try {
                        ColorPalette.Theme themeEnum = ColorPalette.Theme.valueOf(themeName);
                        if (themeEnum != ColorPalette.Theme.RANDOM) { // RANDOM theme (from enum) should not show count
                            int colorCount = ColorPalette.getDefinedColorCount(themeEnum);
                            if (colorCount > 0) {
                                finalDisplayName = String.format("%s (%d colors)", originalDisplayName, colorCount);
                            }
                        }
                        // If themeEnum IS ColorPalette.Theme.RANDOM (i.e. the string "RANDOM" is in getAvailableThemes),
                        // it will skip the count, which is desired.
                        // The "Default (Random Colors)" entry with value "" already handles the general default.
                    } catch (IllegalArgumentException e) {
                        // Theme name not in enum, should not occur if themes come from getAvailableThemes().
                        // Log if necessary. For now, originalDisplayName is used.
                    }
                    model.add(finalDisplayName, themeName);
                }
                return model;
            }
            return new ListBoxModel(); // Return empty if no permission
        }
    }
    
}
