package io.jenkins.plugins.reporter.provider;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportParser;
import io.jenkins.plugins.reporter.parser.ExcelMultiReportParser; // Changed
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ExcelMultiProvider extends Provider { // Changed

    private static final long serialVersionUID = 345678901234L; // New UID
    private static final String ID = "excelmulti"; // Changed

    private ExcelParserConfig excelParserConfig;

    @DataBoundConstructor
    public ExcelMultiProvider() { // Changed
        super();
        this.excelParserConfig = new ExcelParserConfig(); 
    }

    public ExcelParserConfig getExcelParserConfig() {
        return excelParserConfig;
    }

    @DataBoundSetter
    public void setExcelParserConfig(ExcelParserConfig excelParserConfig) {
        this.excelParserConfig = excelParserConfig;
    }

    @Override
    public ReportParser createParser() {
        if (getActualId().equals(getDescriptor().getId())) {
            throw new IllegalArgumentException(Messages.Provider_Error()); // Consider a specific message for excelmulti
        }
        return new ExcelMultiReportParser(getActualId(), getExcelParserConfig()); // Changed
    }

    @Symbol(ID)
    @Extension
    public static class Descriptor extends Provider.ProviderDescriptor {
        public Descriptor() {
            super(ID);
        }
    }
}
