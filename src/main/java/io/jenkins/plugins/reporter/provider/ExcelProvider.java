package io.jenkins.plugins.reporter.provider;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportParser;
import io.jenkins.plugins.reporter.parser.ExcelReportParser;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ExcelProvider extends Provider {

    private static final long serialVersionUID = 834732487834L;
    private static final String ID = "excel";

    private ExcelParserConfig excelParserConfig;

    @DataBoundConstructor
    public ExcelProvider() {
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
            throw new IllegalArgumentException(Messages.Provider_Error());
        }
        return new ExcelReportParser(getActualId(), getExcelParserConfig());
    }

    @Symbol(ID)
    @Extension
    public static class Descriptor extends Provider.ProviderDescriptor {
        public Descriptor() {
            super(ID);
        }
    }
}
