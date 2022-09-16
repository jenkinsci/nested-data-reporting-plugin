package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
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

public class Xml extends Provider {
    
    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "xml";
    
    @DataBoundConstructor
    public Xml() {
        super();
        // empty constructor required for stapler
    }
    
    @Override
    public ReportParser createParser() {
        return new XmlParser();
    }

    /** Descriptor for this provider. */
    @Symbol("xml")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }
    
    public static class XmlParser extends ReportParser {

        private static final long serialVersionUID = 5363254965545196251L;

        @Override
        public ReportDto parse(File file) throws IOException {
            return new ObjectMapper(new XmlFactory()).readerFor(ReportDto.class).readValue(file);
        }
    }
}
