package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.Extension;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;

public class Yaml extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "yaml";
    
    @DataBoundConstructor
    public Yaml() {
        super();
        // empty constructor required for stapler
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
