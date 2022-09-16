package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;

public class Json extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "json";
    
    @DataBoundConstructor
    public Json() {
        super();
        // empty constructor required for stapler
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
        public ReportDto parse(File file) throws IOException {
            return new ObjectMapper(new JsonFactory()).readerFor(ReportDto.class).readValue(file);
        }
    }
}
