package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.ReportDto;
// import io.jenkins.plugins.reporter.model.ReportParser; // No longer directly needed, comes from AbstractReportParserBase
import io.jenkins.plugins.reporter.parser.AbstractReportParserBase; // Added
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public abstract class BaseExcelParser extends AbstractReportParserBase { // Changed superclass

    private static final long serialVersionUID = 1L; // Keep existing or update if major structural change
    // protected static final Logger LOGGER = Logger.getLogger(BaseExcelParser.class.getName()); // Use PARSER_LOGGER from base class
    // No, PARSER_LOGGER in AbstractReportParserBase is for that class. Keep this one for BaseExcelParser specific logs.
    protected static final Logger LOGGER = Logger.getLogger(BaseExcelParser.class.getName());


    protected final ExcelParserConfig config;

    protected BaseExcelParser(ExcelParserConfig config) {
        this.config = config;
    }

    @Override
    public ReportDto parse(File file) throws IOException {
        ReportDto aggregatedReport = new ReportDto();
        aggregatedReport.setItems(new ArrayList<>());
        // aggregatedReport.setParserLog(new ArrayList<>()); // If you add logging messages

        try (InputStream is = new FileInputStream(file)) {
            Workbook workbook;
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else if (fileName.endsWith(".xls")) {
                workbook = new HSSFWorkbook(is);
            } else {
                throw new IllegalArgumentException("File format not supported. Please use .xls or .xlsx: " + file.getName());
            }

            // Logic for iterating sheets will be determined by subclasses.
            // For now, this base `parse` method might be too generic if subclasses
            // have very different sheet iteration strategies (e.g., first vs. all).
            // Consider making this method abstract or providing a hook for sheet selection.
            // For this iteration, let's assume the subclass will guide sheet processing.
            // This method will primarily ensure the workbook is opened and closed correctly.
            
            // This part needs to be implemented by subclasses by calling parseSheet
            // For example, a subclass might iterate through all sheets:
            // for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            //     Sheet sheet = workbook.getSheetAt(i);
            //     ReportDto sheetReport = parseSheet(sheet, sheet.getSheetName(), this.config, createReportId(file.getName(), sheet.getSheetName()));
            //     // Aggregate sheetReport into aggregatedReport
            // }
            // Or a subclass might parse only the first sheet:
            // if (workbook.getNumberOfSheets() > 0) {
            //    Sheet firstSheet = workbook.getSheetAt(0);
            //    aggregatedReport = parseSheet(firstSheet, firstSheet.getSheetName(), this.config, createReportId(file.getName()));
            // }


        } catch (Exception e) {
            LOGGER.severe("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            // aggregatedReport.addParserMessage("Error parsing file: " + e.getMessage());
            throw new IOException("Error parsing Excel file: " + file.getName(), e);
        }
        
        return aggregatedReport; // This will be populated by subclass logic calling parseSheet
    }

    protected abstract ReportDto parseSheet(Sheet sheet, String sheetName, ExcelParserConfig config, String reportId);

    protected String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Or format as needed
                } else {
                    // Format as string, avoiding ".0" for integers
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.format("%d", (long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Evaluate formula and get the cached value as string
                // Be cautious with formula evaluation as it can be complex
                try {
                    return getCellValueAsString(cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateInCell(cell));
                } catch (Exception e) {
                    // Fallback to cached formula result string if evaluation fails
                    LOGGER.warning("Could not evaluate formula in cell " + cell.getAddress() + ": " + e.getMessage());
                    return cell.getCellFormula();
                }
            case BLANK:
            default:
                return "";
        }
    }

    protected List<String> getRowValues(Row row) {
        if (row == null) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (Cell cell : row) {
            values.add(getCellValueAsString(cell));
        }
        return values;
    }

    protected Optional<Integer> findHeaderRow(Sheet sheet, ExcelParserConfig config) {
        // Basic implementation: Assumes first non-empty row is header.
        // TODO: Enhance with config: config.getHeaderRowIndex() or auto-detect
        for (Row row : sheet) {
            if (row == null) continue;
            boolean hasValues = false;
            for (Cell cell : row) {
                if (cell != null && cell.getCellType() != CellType.BLANK && StringUtils.isNotBlank(getCellValueAsString(cell))) {
                    hasValues = true;
                    break;
                }
            }
            if (hasValues) {
                return Optional.of(row.getRowNum());
            }
        }
        return Optional.empty();
    }

    protected List<String> readHeader(Sheet sheet, int headerRowIndex) {
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            return new ArrayList<>();
        }
        return getRowValues(headerRow).stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }
    
    protected Optional<Integer> findFirstDataRow(Sheet sheet, int headerRowIndex, ExcelParserConfig config) {
        // Basic: Assumes data starts on the row immediately after the header.
        // TODO: Enhance with config: config.getDataStartRowIndex() or auto-detect
        int potentialFirstDataRow = headerRowIndex + 1;
        if (potentialFirstDataRow <= sheet.getLastRowNum()) {
             Row row = sheet.getRow(potentialFirstDataRow);
             // Check if the row is not null and not entirely empty
             if (row != null && !isRowEmpty(row)) {
                return Optional.of(potentialFirstDataRow);
             }
        }
        // Fallback: search for the next non-empty row after header
        for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
            Row dataRow = sheet.getRow(i);
            if (dataRow != null && !isRowEmpty(dataRow)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    protected boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        // Check if all cells in the row are blank
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK && StringUtils.isNotBlank(getCellValueAsString(cell))) {
                return false; // Found a non-empty cell
            }
        }
        return true; // All cells are empty or null
    }
}
