package io.jenkins.plugins.reporter.steps.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.reporter.steps.ReportDto;
import io.jenkins.plugins.reporter.steps.Provider;
import io.jenkins.plugins.reporter.steps.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;

public class Yaml extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "yaml";
    private String pattern = StringUtils.EMPTY;

    private String name = StringUtils.EMPTY;
    
    @DataBoundConstructor
    public Yaml() {
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
        return new YamlParser();
    }

    /** Descriptor for this provider. */
    @Symbol({"yaml", "yml"})
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }

    public static class YamlParser extends ReportParser {

        private static final long serialVersionUID = 8953162360286690397L;

        @Override
        public ReportDto parse(File file) throws IOException {
            return new ObjectMapper(new YAMLFactory()).readerFor(ReportDto.class).readValue(file);
        }
    }
}
