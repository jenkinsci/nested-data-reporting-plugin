package io.jenkins.plugins.reporter.steps.provider;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.reporter.model.Result;
import io.jenkins.plugins.reporter.steps.Provider;
import io.jenkins.plugins.reporter.steps.Report;
import io.jenkins.plugins.reporter.steps.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;

public class Json extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "json";
    private String pattern = StringUtils.EMPTY;

    private String name = StringUtils.EMPTY;
    
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

    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public ReportParser createParser() {
        return new JsonParser();
    }

    /** Descriptor for this provider. */
    @Symbol("json")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }

    public static class JsonParser extends ReportParser {
        
        private static final long serialVersionUID = -5067678137282588916L;

        @Override
        public Report parse(File file) throws IOException {
            return new ObjectMapper(new JsonFactory()).readerFor(Report.class).readValue(file);
        }
    }
}
