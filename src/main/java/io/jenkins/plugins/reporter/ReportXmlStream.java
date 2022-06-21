package io.jenkins.plugins.reporter;

import hudson.util.XStream2;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.AbstractXmlStream;

/**
 * Reads {@link Report report} from an XML file.
 *
 * @author Simon Symhoven
 */
public class ReportXmlStream extends AbstractXmlStream<Report> {
    
    ReportXmlStream() {
        super(Report.class);
    }
    
    @Override
    public Report createDefaultValue() {
        return new Report();
    }

    @Override
    protected void configureXStream(XStream2 xStream) {
        xStream.alias("dataReport", Report.class);
    }
}
