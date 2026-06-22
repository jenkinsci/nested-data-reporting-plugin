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
import java.util.ArrayList;
import java.util.List;

/**
 * Provider for multi-sheet Excel (XLSX) files with header consistency enforcement
 * This provider parses all sheets in an Excel file, enforcing header consistency across sheets.
 * Only sheets with identical headers (compared to the first valid sheet) will be processed.
 */
public class ExcelMulti extends AbstractExcelProvider {

    private static final long serialVersionUID = 1845129735392309267L;

    private static final String ID = "excelMulti";

    /** Default constructor */
    @DataBoundConstructor
    public ExcelMulti() {
        super();
        // empty constructor required for stapler
    }

    /** Creates an ExcelMulti parser */
    @Override
    public ReportParser createParser() {
        if (getActualId().equals(getDescriptor().getId())) {
            throw new IllegalArgumentException(Messages.Provider_Error());
        }

        return new ExcelMultiParser(getActualId());
    }

    /** Descriptor for this provider */
    @Symbol("excelMulti")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates a descriptor instance */
        public Descriptor() {
            super(ID);
        }
    }

    /**
     * Parser for multi-sheet Excel files with header consistency enforcement
     */
    public static class ExcelMultiParser extends AbstractExcelParser {

        private static final long serialVersionUID = 3724789235028726491L;

        /** Constructor */
        public ExcelMultiParser(String id) {
            super(id);
        }

        /**
         * Parses an Excel file with multiple sheets and creates a ReportDto
         * Only processes sheets with consistent headers (identical to the first valid sheet)
         */
        @Override
        public ReportDto parse(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                
                TabularData result = null;
                List<String> referenceHeader = null;
                int totalSheets = workbook.getNumberOfSheets();
                int processedSheets = 0;
                int skippedSheets = 0;
                List<String> processedSheetNames = new ArrayList<>();
                List<String> skippedSheetNames = new ArrayList<>();
                List<List<String>> allRows = new ArrayList<>();
                
                // Add an initial parser message about number of sheets
                parserMessages.add(String.format("Excel file contains %d sheets", totalSheets));
                
                // First pass: validate sheets and collect all rows
                for (int i = 0; i < totalSheets; i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    
                    // Extract data from the sheet
                    TabularData sheetData = extractSheetData(sheet, referenceHeader);
                    
                    // If the sheet contains valid data
                    if (sheetData != null) {
                        // If it's the first valid sheet, use it as reference
                        if (referenceHeader == null) {
                            referenceHeader = sheetData.getHeader();
                            allRows.addAll(sheetData.getRows());
                            processedSheets++;
                            processedSheetNames.add(sheetName);
                            parserMessages.add(String.format("Processing sheet '%s' as reference sheet with %d columns: %s", 
                                sheetName, referenceHeader.size(), String.join(", ", referenceHeader)));
                        } else {
                            // For subsequent sheets, add rows to the collection
                            allRows.addAll(sheetData.getRows());
                            processedSheets++;
                            processedSheetNames.add(sheetName);
                            parserMessages.add(String.format("Processing sheet '%s' - adding %d rows", 
                                sheetName, sheetData.getRows().size()));
                        }
                    } else {
                        skippedSheets++;
                        skippedSheetNames.add(sheetName);
                        parserMessages.add(String.format("Skipped sheet '%s' - Invalid format or no data", sheetName));
                    }
                }
                
                // Create final TabularData with all rows
                if (referenceHeader != null && !allRows.isEmpty()) {
                    result = new TabularData(getId(), referenceHeader, allRows);
                }
                
                // Add summary information to parser messages
                parserMessages.add(String.format("Processed %d sheets: %s", 
                    processedSheets, String.join(", ", processedSheetNames)));
                
                if (skippedSheets > 0) {
                    parserMessages.add(String.format("Skipped %d sheets: %s", 
                        skippedSheets, String.join(", ", skippedSheetNames)));
                }
                
                // If no sheet contains valid data
                if (result == null) {
                    throw new IOException("No valid data found in Excel file. All sheets were skipped.");
                }
                
                parserMessages.add(String.format("Total rows collected: %d", allRows.size()));
                
                // Process and return the final tabular data
                return result.processData(parserMessages);
            }
        }
    }
}

