package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.poi.ss.usermodel.*;
// import org.apache.commons.lang3.StringUtils; // No longer directly used here as logic moved to base
// import org.apache.commons.lang3.math.NumberUtils; // No longer directly used here

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
// Ensure WorkbookFactory is imported if used:
import org.apache.poi.ss.usermodel.WorkbookFactory;


public class ExcelReportParser extends BaseExcelParser {

    private static final long serialVersionUID = 923478237482L;
    private final String id;
    private List<String> parserMessages;

    public ExcelReportParser(String id, ExcelParserConfig config) {
        super(config);
        this.id = id;
        this.parserMessages = new ArrayList<>();
    }

    @Override
    public ReportDto parse(File file) throws IOException {
        ReportDto reportDto = new ReportDto();
        reportDto.setId(this.id); 
        reportDto.setItems(new ArrayList<>());

        try (InputStream is = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(is)) { 

            if (workbook.getNumberOfSheets() == 0) {
                this.parserMessages.add("Excel file has no sheets: " + file.getName());
                LOGGER.warning("Excel file has no sheets: " + file.getName());
                reportDto.setParserLogMessages(this.parserMessages);
                return reportDto;
            }

            Sheet firstSheet = workbook.getSheetAt(0);
            ReportDto sheetReport = parseSheet(firstSheet, firstSheet.getSheetName(), this.config, this.id);
            sheetReport.setParserLogMessages(this.parserMessages); 
            return sheetReport;

        } catch (Exception e) {
            this.parserMessages.add("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            LOGGER.severe("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            reportDto.setParserLogMessages(this.parserMessages);
            return reportDto; 
        }
    }

    @Override
    protected ReportDto parseSheet(Sheet sheet, String sheetName, ExcelParserConfig config, String reportId) {
        ReportDto report = new ReportDto();
        report.setId(reportId);
        report.setItems(new ArrayList<>());

        Optional<Integer> headerRowIndexOpt = findHeaderRow(sheet, config);
        if (!headerRowIndexOpt.isPresent()) {
            this.parserMessages.add(String.format("No header row found in sheet: %s", sheetName));
            LOGGER.warning(String.format("No header row found in sheet: %s", sheetName));
            return report;
        }
        int headerRowIndex = headerRowIndexOpt.get();

        List<String> header = readHeader(sheet, headerRowIndex);
        if (header.isEmpty() || header.size() < 2) {
            this.parserMessages.add(String.format("Empty or insufficient header (found %d columns, requires at least 2) in sheet: %s at row %d", header.size(), sheetName, headerRowIndex + 1));
            LOGGER.warning(String.format("Empty or insufficient header in sheet: %s at row %d", sheetName, headerRowIndex + 1));
            return report;
        }

        Optional<Integer> firstDataRowIndexOpt = findFirstDataRow(sheet, headerRowIndex, config);
        if (!firstDataRowIndexOpt.isPresent()) {
            this.parserMessages.add(String.format("No data rows found after header in sheet: %s", sheetName));
            LOGGER.info(String.format("No data rows found after header in sheet: %s", sheetName));
            return report;
        }
        int firstDataRowIndex = firstDataRowIndexOpt.get();
        
        Row actualFirstDataRow = sheet.getRow(firstDataRowIndex);
        List<String> firstDataRowValues = null;
        if (actualFirstDataRow != null && !isRowEmpty(actualFirstDataRow)) {
            firstDataRowValues = getRowValues(actualFirstDataRow);
        }

        int colIdxValueStart = detectColumnStructure(header, firstDataRowValues, this.parserMessages, "Excel");
        if (colIdxValueStart == -1) {
            // Error already logged by detectColumnStructure
            return report; 
        }

        for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
            Row currentRow = sheet.getRow(i);
            // No need to explicitly check isRowEmpty here, parseRowToItems will handle it.
            // It will log if it skips an empty row.
            List<String> rowValues = getRowValues(currentRow); 
            // The reportId is used as the baseItemIdPrefix for ExcelReportParser
            parseRowToItems(report, rowValues, header, colIdxValueStart, reportId, this.parserMessages, "Excel", i);
        }
        return report;
    }
}
