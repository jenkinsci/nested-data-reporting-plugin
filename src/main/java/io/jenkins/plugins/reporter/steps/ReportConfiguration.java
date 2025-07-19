package io.jenkins.plugins.reporter.steps;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.util.JenkinsFacade;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

/**
 * Configuration for a single report in a FreeStyle project.
 */
public class ReportConfiguration extends AbstractDescribableImpl<ReportConfiguration> {
    
    private String name;
    private Provider provider;
    private String displayType = "dual";
    
    @DataBoundConstructor
    public ReportConfiguration() {
        // empty constructor required for Stapler
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
    
    @Extension
    public static class DescriptorImpl extends Descriptor<ReportConfiguration> {
        private static final JenkinsFacade JENKINS = new JenkinsFacade();
        
        @Override
        public String getDisplayName() {
            return "Report Configuration";
        }
        
        @POST
        public FormValidation doCheckName(@QueryParameter("name") String name) {
            if (StringUtils.isEmpty(name)) {
                return FormValidation.error("Field 'name' is required.");
            }
            return FormValidation.ok();
        }
        
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
