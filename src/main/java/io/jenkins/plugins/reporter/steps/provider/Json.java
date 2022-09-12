package io.jenkins.plugins.reporter.steps.provider;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.reporter.steps.Provider;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class Json extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "json";
    private String pattern = StringUtils.EMPTY;

    @DataBoundConstructor
    public Json() {
        super();
        // empty constructor required for stapler
    }
    
    /**
     * Sets the Ant file-set pattern of files to work with.
     * scanned.
     *
     * @param pattern
     *         the pattern to use
     */
    @DataBoundSetter
    public void setPattern(final String pattern) {
        this.pattern = pattern;
    }

    @CheckForNull
    public String getPattern() {
        return pattern;
    }

    /** Descriptor for this provider. */
    @Symbol("Json")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }
}
