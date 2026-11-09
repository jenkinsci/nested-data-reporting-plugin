package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import io.jenkins.plugins.reporter.util.TabularData;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for Excel-based providers
 * Contains common functionality for Excel file parsing
 */
public abstract class AbstractExcelProvider extends Provider {

    private static final long serialVersionUID = 5463781055792899347L;

    /** Default constructor */
    protected AbstractExcelProvider() {
        super();
    }

    /**
     * Base class for Excel parsers
     */
    public abstract static class AbstractExcelParser extends ReportParser {
        
        private static final long serialVersionUID = -8689695008930386641L;
        protected final String id;
        protected List<String> parserMessages;

        /** Constructor */
        protected AbstractExcelParser(String id) {
            super();
            this.id = id;
            this.parserMessages = new ArrayList<>();
        }

        /** Returns the parser identifier */
        public String getId() {
            return id;
        }

        /** Detects the table position in an Excel sheet */
        protected TablePosition detectTablePosition(Sheet sheet) {
            int startRow = -1;
            int startCol = -1;
            int maxNonEmptyConsecutiveCells = 0;
            int headerRowIndex = -1;

            // Check the first 20 rows to find the table start
            int maxRowsToCheck = Math.min(20, sheet.getLastRowNum() + 1);
            
            for (int rowIndex = 0; rowIndex < maxRowsToCheck; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                int nonEmptyConsecutiveCells = 0;
                int firstNonEmptyCellIndex = -1;
                
                // Check the cells in the row
                for (int colIndex = 0; colIndex < 100; colIndex++) { // Arbitrary limit of 100 columns
                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null) {
                        if (firstNonEmptyCellIndex == -1) {
                            firstNonEmptyCellIndex = colIndex;
                        }
                        nonEmptyConsecutiveCells++;
                    } else if (firstNonEmptyCellIndex != -1) {
                        // Found an empty cell after non-empty cells
                        break;
                    }
                }
                
                // If we found a row with more consecutive non-empty cells than before
                if (nonEmptyConsecutiveCells > maxNonEmptyConsecutiveCells && nonEmptyConsecutiveCells >= 2) {
                    maxNonEmptyConsecutiveCells = nonEmptyConsecutiveCells;
                    headerRowIndex = rowIndex;
                    startCol = firstNonEmptyCellIndex;
                }
            }
            
            // If we found a potential header
            if (headerRowIndex != -1) {
                startRow = headerRowIndex;
            } else {
                // Default to the first row
                startRow = 0;
                startCol = 0;
            }
            
            return new TablePosition(startRow, startCol);
        }

        /** Checks if a row is empty */
        protected boolean isRowEmpty(Row row, int startCol, int columnCount) {
            if (row == null) return true;
            
            for (int i = startCol; i < startCol + columnCount; i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        /** Extracts data from an Excel sheet */
        protected TabularData extractSheetData(Sheet sheet, List<String> referenceHeader) {
            // Detect table position
            TablePosition tablePos = detectTablePosition(sheet);
            int startRow = tablePos.getStartRow();
            int startCol = tablePos.getStartCol();
            
            // Get the header row
            Row headerRow = sheet.getRow(startRow);
            if (headerRow == null) {
                parserMessages.add(String.format("Skipped sheet '%s' - No header row found", sheet.getSheetName()));
                return null;
            }
            
            // Extract headers
            List<String> header = new ArrayList<>();
            int lastCol = headerRow.getLastCellNum();
            for (int colIdx = startCol; colIdx < lastCol; colIdx++) {
                Cell cell = headerRow.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell != null) {
                    header.add(getCellValueAsString(cell));
                } else {
                    // If we find an empty cell in the header, stop
                    break;
                }
            }
            
            // Check that the header has at least 2 columns
            if (header.size() < 2) {
                parserMessages.add(String.format("Skipped sheet '%s' - Header has less than 2 columns", sheet.getSheetName()));
                return null;
            }
            
            // If a reference header is provided, check that it matches
            if (referenceHeader != null && !header.equals(referenceHeader)) {
                parserMessages.add(String.format("Skipped sheet '%s' - Header does not match reference header", sheet.getSheetName()));
                return null;
            }
            
            // Extract data rows
            List<List<String>> rows = new ArrayList<>();
            int headerColumnCount = header.size();
            
            for (int rowIdx = startRow + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row dataRow = sheet.getRow(rowIdx);
                
                // Skip if row is null
                if (dataRow == null) {
                    continue;
                }
                
                List<String> rowData = new ArrayList<>();
                boolean hasData = false;
                
                // Extract data from the row
                for (int colIdx = startCol; colIdx < startCol + headerColumnCount; colIdx++) {
                    Cell cell = dataRow.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = getCellValueAsString(cell);
                    rowData.add(value);
                    if (cell != null && !value.trim().isEmpty()) {
                        hasData = true;
                    }
                }
                
                // Only add rows that have at least one non-empty cell
                if (hasData) {
                    rows.add(rowData);
                }
            }
            
            // Only return data if we found any rows
            return rows.isEmpty() ? null : new TabularData(id, header, rows);
        }

        /** Converts an Excel cell value to a string */
        protected String getCellValueAsString(Cell cell) {
            if (cell == null) {
                return "";
            }
            
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        // To avoid scientific notation display
                        double value = cell.getNumericCellValue();
                        if (value == Math.floor(value)) {
                            return String.format("%.0f", value);
                        } else {
                            return String.valueOf(value);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (IllegalStateException e) {
                        try {
                            return String.valueOf(cell.getStringCellValue());
                        } catch (IllegalStateException e2) {
                            return "#ERROR";
                        }
                    }
                default:
                    return "";
            }
        }

        /** Internal class to store the position of a table in an Excel sheet */
        protected static class TablePosition {
            private final int startRow;
            private final int startCol;
            
            public TablePosition(int startRow, int startCol) {
                this.startRow = startRow;
                this.startCol = startCol;
            }
            
            public int getStartRow() {
                return startRow;
            }
            
            public int getStartCol() {
                return startCol;
            }
        }
    }
}

