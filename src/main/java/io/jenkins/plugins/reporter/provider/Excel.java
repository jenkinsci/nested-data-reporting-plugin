package io.jenkins.plugins.reporter.provider;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import io.jenkins.plugins.reporter.util.TabularData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Provider for Excel (XLSX) files
 */
public class Excel extends AbstractExcelProvider {

    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "excel";

    /** Default constructor */
    @DataBoundConstructor
    public Excel() {
        super();
        // empty constructor required for stapler
    }

    /** Creates an Excel parser */
    @Override
    public ReportParser createParser() {
        if (getActualId().equals(getDescriptor().getId())) {
            throw new IllegalArgumentException(Messages.Provider_Error());
        }

        return new ExcelParser(getActualId());
    }

    /** Descriptor for this provider */
    @Symbol("excel")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates a descriptor instance */
        public Descriptor() {
            super(ID);
        }
    }

    /**
     * Parser for Excel files
     */
    public static class ExcelParser extends AbstractExcelParser {

        private static final long serialVersionUID = -8689695008930386641L;

        /** Constructor */
        public ExcelParser(String id) {
            super(id);
        }

        /** Parses an Excel file and creates a ReportDto */
        @Override
        public ReportDto parse(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                
                TabularData result = null;
                List<String> referenceHeader = null;
                
                // Process all sheets in the workbook
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    
                    // Extract data from the sheet
                    TabularData sheetData = extractSheetData(sheet, referenceHeader);
                    
                    // If the sheet contains valid data
                    if (sheetData != null) {
                        // If it's the first valid sheet, use it as reference
                        if (result == null) {
                            result = sheetData;
                            referenceHeader = sheetData.getHeader();
                        } else {
                            // Otherwise, add the sheet rows to the result
                            result.getRows().addAll(sheetData.getRows());
                        }
                    }
                }
                
                // If no sheet contains valid data
                if (result == null) {
                    throw new IOException("No valid data found in Excel file");
                }
                
                // Process tabular data
                return result.processData(parserMessages);
            }
        }
    }
}
