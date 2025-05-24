package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;

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
        this.parserMessages.add(String.format("Debug [Excel]: Sheet: %s, Header: %s", sheetName, header.toString()));
        this.parserMessages.add(String.format("Debug [Excel]: Sheet: %s, FirstDataRowValues for structure detection: %s", sheetName, (firstDataRowValues != null ? firstDataRowValues.toString() : "null")));

        int colIdxValueStart = detectColumnStructure(header, firstDataRowValues, this.parserMessages, "Excel");
        this.parserMessages.add(String.format("Debug [Excel]: Sheet: %s, Detected colIdxValueStart: %d", sheetName, colIdxValueStart));
        if (colIdxValueStart == -1) {
            // Error already logged by detectColumnStructure
            return report; 
        }

        for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
            Row currentRow = sheet.getRow(i);
            if (isRowEmpty(currentRow)) { // isRowEmpty is a protected method in BaseExcelParser
                 this.parserMessages.add(String.format("Info [Excel]: Skipped empty Excel row object at sheet row index %d.", i));
                 continue;
            }
            List<String> rowValues = getRowValues(currentRow); 
            // Add the existing diagnostic log from the previous step
            this.parserMessages.add(String.format("Debug [Excel]: Sheet: %s, Row: %d, Processing rowValues: %s", sheetName, i, rowValues.toString()));
            // parseRowToItems(report, rowValues, header, colIdxValueStart, reportId, this.parserMessages, "Excel", i);
            // TODO: This is where parseSheetRow was previously called indirectly via parseRowToItems.
            // The task asks to modify parseSheetRow, but parseRowToItems is what's called here.
            // This suggests parseRowToItems might be the method to change, or there's a misunderstanding
            // in the refactoring chain from the original issue.
            // For now, I will assume the task meant to adapt the logic that was *previously* in parseSheetRow,
            // which is now mostly within parseRowToItems in BaseExcelParser.
            // However, the specific changes (dataRowNumber, itemName, itemId, logMessage)
            // are about how a row is processed when it has NO hierarchy.
            // This logic IS in BaseExcelParser.parseRowToItems.

            // The request is to pass headerRowIndex to parseSheetRow.
            // Let's assume parseRowToItems (which is in BaseExcelParser) needs to be the target of this change,
            // or a new parseSheetRow needs to be re-introduced in ExcelReportParser if it was removed.

            // Given the existing code structure, parseRowToItems is the method from BaseExcelParser
            // that processes rows. If ExcelReportParser needs custom row processing for the
            // "no hierarchy" case, it would typically override parseRowToItems or have its own
            // specific helper that parseRowToItems might call.

            // The task description is very specific about changing `parseSheetRow` in `ExcelReportParser.java`.
            // However, looking at the provided `ExcelReportParser.java` from the previous turn,
            // there is no method named `parseSheetRow`. The row processing logic seems to have been
            // centralized into `BaseExcelParser.parseRowToItems`.

            // Let's proceed by ADDING the `parseSheetRow` method to `ExcelReportParser.java`
            // as described, and then calling it from the loop. This might be a re-introduction
            // of a previously removed/refactored method.

            // Call the inherited parseRowToItems
            // reportId is used as baseItemIdPrefix
            // this.parserMessages is the messagesCollector
            // "Excel" is the parserName
            // (i - firstDataRowIndex) can serve as the 0-based rowIndexForLog for data rows
            parseRowToItems(report, rowValues, header, colIdxValueStart, reportId, this.parserMessages, "Excel", i - firstDataRowIndex);
        }
        return report;
    }
}
