package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.poi.ss.usermodel.*;
// import org.apache.commons.lang3.StringUtils; // No longer directly used here
// import org.apache.commons.lang3.math.NumberUtils; // No longer directly used here
import org.apache.poi.ss.usermodel.WorkbookFactory; // Ensure this is present

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public class ExcelMultiReportParser extends BaseExcelParser { // Changed

    private static final long serialVersionUID = 456789012345L; // New UID
    private final String id;
    private List<String> parserMessages;
    private List<String> overallHeader = null; 

    public ExcelMultiReportParser(String id, ExcelParserConfig config) { // Changed
        super(config);
        this.id = id;
        this.parserMessages = new ArrayList<>();
    }

    @Override
    public ReportDto parse(File file) throws IOException {
        this.overallHeader = null; 
        // this.parserMessages.clear(); // Clear if instance is reused; assume new instance for now.

        ReportDto aggregatedReport = new ReportDto();
        aggregatedReport.setId(this.id);
        aggregatedReport.setItems(new ArrayList<>());

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                this.parserMessages.add("Excel file has no sheets: " + file.getName());
                LOGGER.warning("Excel file has no sheets: " + file.getName());
                aggregatedReport.setParserLogMessages(this.parserMessages);
                return aggregatedReport;
            }

            for (Sheet sheet : workbook) {
                String cleanSheetName = sheet.getSheetName().replaceAll("[^a-zA-Z0-9_.-]", "_");
                ReportDto sheetReport = parseSheet(sheet, sheet.getSheetName(), this.config, this.id + "::" + cleanSheetName);
                
                if (sheetReport != null && sheetReport.getItems() != null) {
                    for (Item item : sheetReport.getItems()) {
                        if (aggregatedReport.getItems() == null) aggregatedReport.setItems(new java.util.ArrayList<>()); // Defensive
                        aggregatedReport.getItems().add(item);
                    }
                }
            }

            aggregatedReport.setParserLogMessages(this.parserMessages);
            return aggregatedReport;

        } catch (Exception e) {
            this.parserMessages.add("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            LOGGER.severe("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            aggregatedReport.setParserLogMessages(this.parserMessages);
            return aggregatedReport;
        }
    }

    @Override
    protected ReportDto parseSheet(Sheet sheet, String sheetName, ExcelParserConfig config, String reportId) {
        ReportDto report = new ReportDto();
        report.setId(reportId);
        report.setItems(new ArrayList<>());

        Optional<Integer> headerRowIndexOpt = findHeaderRow(sheet, config);
        if (!headerRowIndexOpt.isPresent()) {
            this.parserMessages.add(String.format("No header row found in sheet: '%s'", sheetName));
            LOGGER.warning(String.format("No header row found in sheet: '%s'", sheetName));
            return report;
        }
        int headerRowIndex = headerRowIndexOpt.get();

        List<String> currentSheetHeader = readHeader(sheet, headerRowIndex);
        if (currentSheetHeader.isEmpty() || currentSheetHeader.size() < 2) {
            this.parserMessages.add(String.format("Empty or insufficient header (found %d columns, requires at least 2) in sheet: '%s' at row %d. Skipping sheet.", currentSheetHeader.size(), sheetName, headerRowIndex + 1));
            LOGGER.warning(String.format("Empty or insufficient header in sheet: '%s' at row %d. Skipping sheet.", sheetName, headerRowIndex + 1));
            return report;
        }

        // Column Consistency Check
        if (this.overallHeader == null) {
            this.overallHeader = new ArrayList<>(currentSheetHeader); // Set if this is the first valid header encountered
            this.parserMessages.add(String.format("Info: Using header from sheet '%s' as the reference for column consistency: %s", sheetName, this.overallHeader.toString()));
        } else {
            if (!this.overallHeader.equals(currentSheetHeader)) {
                String msg = String.format("Error: Sheet '%s' has an inconsistent header. Expected: %s, Found: %s. Skipping this sheet.", sheetName, this.overallHeader.toString(), currentSheetHeader.toString());
                this.parserMessages.add(msg);
                LOGGER.severe(msg);
                return report; 
            }
        }

        Optional<Integer> firstDataRowIndexOpt = findFirstDataRow(sheet, headerRowIndex, config);
        if (!firstDataRowIndexOpt.isPresent()) {
            this.parserMessages.add(String.format("No data rows found after header in sheet: '%s'", sheetName));
            LOGGER.info(String.format("No data rows found after header in sheet: '%s'", sheetName));
            return report;
        }
        int firstDataRowIndex = firstDataRowIndexOpt.get();
        
        Row actualFirstDataRow = sheet.getRow(firstDataRowIndex);
        List<String> firstDataRowValues = null;
        if (actualFirstDataRow != null && !isRowEmpty(actualFirstDataRow)) {
            firstDataRowValues = getRowValues(actualFirstDataRow);
        }

        int colIdxValueStart = detectColumnStructure(currentSheetHeader, firstDataRowValues, this.parserMessages, "ExcelMulti");
        if (colIdxValueStart == -1) {
            // Error already logged by detectColumnStructure
            return report;
        }

        // Data Processing Loop
        for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
            Row currentRow = sheet.getRow(i);
            // parseRowToItems will handle empty rows and log them.
            List<String> rowValues = getRowValues(currentRow);
            // reportId here is already sheet-specific (e.g., this.id + "::" + cleanSheetName)
            parseRowToItems(report, rowValues, currentSheetHeader, colIdxValueStart, reportId, this.parserMessages, "ExcelMulti", i);
        }
        return report;
    }
}
