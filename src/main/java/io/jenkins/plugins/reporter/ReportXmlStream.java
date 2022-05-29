package io.jenkins.plugins.reporter;

import hudson.util.XStream2;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.util.AbstractXmlStream;

public class ReportXmlStream extends AbstractXmlStream<Report> {
    
    public ReportXmlStream() {
        super(Report.class);
    }

    public Report createDefaultValue() {
        return new Report();
    }

    @Override
    protected void configureXStream(XStream2 xStream) {
        xStream.alias("reports", Report.class);
    }
}
