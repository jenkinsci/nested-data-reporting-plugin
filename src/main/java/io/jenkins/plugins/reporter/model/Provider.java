package io.jenkins.plugins.reporter.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.FilePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Run;
import io.jenkins.plugins.reporter.FilesScanner;
import io.jenkins.plugins.reporter.LogHandler;
import io.jenkins.plugins.util.JenkinsFacade;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serializable;

public abstract class Provider extends AbstractDescribableImpl<Provider>  implements Serializable {
    
    private static final long serialVersionUID = -1356603376948787474L;
    
    private String name = StringUtils.EMPTY;

    private String pattern = StringUtils.EMPTY;

    private JenkinsFacade jenkins = new JenkinsFacade();

    /**
     * Called after de-serialization to retain backward compatibility.
     *
     * @return this
     */
    protected Object readResolve() {
        jenkins = new JenkinsFacade();

        return this;
    }

    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    
    /**
     * Sets the Ant file-set pattern of files to work with. If the pattern is undefined then the console log is
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

    /**
     * Returns the {@link Symbol} name of this provider.
     *
     * @return the name of this provider, or "undefined" if no symbol has been defined
     */
    public String getSymbolName() {
        return getDescriptor().getSymbolName();
    }
    
    public abstract ReportParser createParser();
    
    @Override
    public ProviderDescriptor getDescriptor() {
        return (ProviderDescriptor) jenkins.getDescriptorOrDie(getClass());
    }

    public Report scan(final Run<?, ?> run, final FilePath workspace, final LogHandler logger) {
        return scanInWorkspace(workspace, getPattern(), logger);
    }
    
    private Report scanInWorkspace(final FilePath workspace, final String pattern, final LogHandler logger) {
        try {
            Report report = workspace.act(
                    new FilesScanner(getPattern(), createParser()));

            logger.log(report);

            return report;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    public abstract static class ProviderDescriptor extends Descriptor<Provider> {
        
        private final String defaultId;

        /**
         * Creates a new instance of {@link ProviderDescriptor} with the given ID.
         *
         * @param defaultId
         *         the unique ID of the tool
         */
        protected ProviderDescriptor(final String defaultId) {
            super();

            this.defaultId = defaultId;
        }

        @Override
        public String getId() {
            return defaultId;
        }

        /**
         * Returns the default name of this tool.
         *
         * @return the name
         */
        public String getName() {
            return getDisplayName();
        }

        /**
         * Returns the {@link Symbol} name of this provider.
         *
         * @return the name of this provider, or "undefined" if no symbol has been defined
         */
        public String getSymbolName() {
            Symbol annotation = getClass().getAnnotation(Symbol.class);

            if (annotation != null) {
                String[] symbols = annotation.value();
                if (symbols.length > 0) {
                    return symbols[0];
                }
            }
            return "undefined";
        }
    }
}
