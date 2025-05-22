package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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
                reportDto.setParserLog(this.parserMessages);
                return reportDto;
            }

            Sheet firstSheet = workbook.getSheetAt(0);
            ReportDto sheetReport = parseSheet(firstSheet, firstSheet.getSheetName(), this.config, this.id);
            sheetReport.setParserLog(this.parserMessages); 
            return sheetReport;

        } catch (Exception e) {
            this.parserMessages.add("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            LOGGER.severe("Error parsing Excel file " + file.getName() + ": " + e.getMessage());
            reportDto.setParserLog(this.parserMessages);
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
        
        int colIdxValueStart = -1; 
        boolean structureDetected = false;

        for (int tempRowIdx = firstDataRowIndex; tempRowIdx <= sheet.getLastRowNum(); tempRowIdx++) {
            Row r = sheet.getRow(tempRowIdx);
            if (isRowEmpty(r)) continue;
            List<String> rv = getRowValues(r);
            if (rv.isEmpty()) continue;

            int determinedColIdxValueStart = 0; 
            if (!rv.isEmpty()) {
                for (int cIdx = header.size() - 1; cIdx >= 0; cIdx--) {
                    String cellVal = (cIdx < rv.size()) ? rv.get(cIdx) : "";
                    if (NumberUtils.isCreatable(cellVal)) {
                        determinedColIdxValueStart = cIdx;
                    } else {
                        if (determinedColIdxValueStart > 0 && cIdx < determinedColIdxValueStart) { 
                            break; 
                        }
                    }
                }
                if (determinedColIdxValueStart == 0 && !rv.isEmpty()) {
                    if (NumberUtils.isCreatable(rv.get(0))) {
                         if(header.size() == 1) {
                            this.parserMessages.add(String.format(
                                "Warning: Sheet '%s', row %d: Data seems to be a single numeric column ('%s'). Item names will be generic.",
                                sheetName, tempRowIdx + 1, header.get(0)));
                         }
                    } else { 
                        if (header.size() > 1) {
                            determinedColIdxValueStart = header.size() - 1; 
                            this.parserMessages.add(String.format(
                                "Warning: No numeric columns auto-detected in sheet '%s' at row %d based on content. " +
                                "Assuming last column ('%s') is value, and others are hierarchy.",
                                sheetName, tempRowIdx + 1, header.get(determinedColIdxValueStart)));
                        } else { 
                            this.parserMessages.add(String.format("Warning: Sheet '%s', row %d: Single text column ('%s') found. No numeric data columns detected.", sheetName, tempRowIdx + 1, header.get(0)));
                        }
                    }
                }
            }
            
            colIdxValueStart = determinedColIdxValueStart;

            if (colIdxValueStart < header.size() && colIdxValueStart >=0) {
                 this.parserMessages.add(String.format("Detected structure in sheet '%s': Hierarchy columns: 0 to %d, Value columns: %d to %d.",
                    sheetName, Math.max(0, colIdxValueStart -1), colIdxValueStart, header.size() - 1));
                 structureDetected = true;
            } else if (header.size() > 0) { 
                 this.parserMessages.add(String.format("Error: Could not reliably determine data structure (colIdxValueStart %d vs header size %d) in sheet '%s'.", colIdxValueStart, header.size(), sheetName));
                 return report; 
            } else { 
                 return report;
            }
            break; 
        }

        if (!structureDetected) {
            this.parserMessages.add(String.format("Warning: Could not detect data structure in sheet '%s'. No processable data rows found or structure ambiguous.", sheetName));
            return report; 
        }

        for (int i = firstDataRowIndex; i <= sheet.getLastRowNum(); i++) {
            Row currentRow = sheet.getRow(i);
            if (isRowEmpty(currentRow)) {
                 this.parserMessages.add(String.format("Skipped empty row %d in sheet '%s'", i + 1, sheetName));
                 continue;
            }

            List<String> rowValues = getRowValues(currentRow);
            
            if (rowValues.size() < colIdxValueStart && colIdxValueStart > 0) { 
                 this.parserMessages.add(String.format("Skipped row %d in sheet '%s' - Row has %d cells, but hierarchy part expects at least %d based on detected structure.", i + 1, sheetName, rowValues.size(), colIdxValueStart));
                 continue;
            }

            String parentId = "report";
            Item lastItem = null;
            boolean lastItemAddedToHierarchy = false;
            LinkedHashMap<String, Integer> resultValues = new LinkedHashMap<>();
            boolean emptyCellInHierarchyPart = false;
            String currentItemCombinedId = "";

            for (int colIdx = 0; colIdx < header.size(); colIdx++) {
                String headerName = header.get(colIdx);
                String cellValue = (colIdx < rowValues.size()) ? rowValues.get(colIdx) : "";

                if (colIdx < colIdxValueStart) { 
                    if (StringUtils.isBlank(cellValue)) {
                        if (colIdx == 0) {
                            this.parserMessages.add(String.format("Skipped row %d in sheet '%s' - First hierarchy column (header '%s') is empty.", i + 1, sheetName, headerName));
                            emptyCellInHierarchyPart = true;
                            break; 
                        }
                        // Allow subsequent blank hierarchy cells, but they might make IDs less unique or meaningful
                        this.parserMessages.add(String.format("Info: Row %d, Col %d (Header '%s') in sheet '%s' is part of hierarchy and is blank.", i + 1, colIdx + 1, headerName, sheetName));
                    } else if (NumberUtils.isCreatable(cellValue)) {
                         this.parserMessages.add(String.format("Info: Row %d, Col %d (Header '%s') in sheet '%s' is part of hierarchy but is numeric-like ('%s'). Using as string.", i + 1, colIdx + 1, headerName, sheetName, cellValue));
                    }
                    
                    if (emptyCellInHierarchyPart && StringUtils.isNotBlank(cellValue)) {
                        this.parserMessages.add(String.format("Skipped row %d in sheet '%s' - Non-empty value ('%s') found after a blank cell in the hierarchy part.", i + 1, sheetName, cellValue));
                        emptyCellInHierarchyPart = true; 
                        break;
                    }
                    
                    currentItemCombinedId += cellValue.replaceAll("[^a-zA-Z0-9_-]", "_") + "_";
                    String itemId = StringUtils.removeEnd(currentItemCombinedId, "_");
                    if (StringUtils.isBlank(itemId)) itemId = "unnamed_item_" + colIdx;


                    Optional<Item> parentOpt = report.findItem(parentId, report.getItems());
                    Item currentItem = new Item();
                    currentItem.setId(itemId);
                    currentItem.setName(StringUtils.isBlank(cellValue) ? "(blank)" : cellValue);
                    lastItemAddedToHierarchy = false;

                    if (parentOpt.isPresent()) {
                        Item p = parentOpt.get();
                        if (p.getItems() == null) p.setItems(new ArrayList<>());
                        Optional<Item> existingItem = p.getItems().stream().filter(it -> it.getId().equals(itemId)).findFirst();
                        if (!existingItem.isPresent()) {
                            p.addItem(currentItem);
                            lastItemAddedToHierarchy = true;
                        }
                        lastItem = existingItem.orElse(currentItem);
                    } else {
                         Optional<Item> existingRootItem = report.getItems().stream().filter(it -> it.getId().equals(itemId)).findFirst();
                         if (!existingRootItem.isPresent()) {
                            report.addItem(currentItem);
                            lastItemAddedToHierarchy = true;
                         }
                         lastItem = existingRootItem.orElse(currentItem);
                    }
                    parentId = itemId;
                } else { 
                    Number numValue = 0;
                    if (NumberUtils.isCreatable(cellValue)) {
                        numValue = NumberUtils.createNumber(cellValue);
                    } else if (StringUtils.isNotBlank(cellValue)) {
                         this.parserMessages.add(String.format("Warning: Non-numeric value '%s' in data column '%s' at row %d, col %d, sheet '%s'. Using 0.", cellValue, headerName, i + 1, colIdx + 1, sheetName));
                    }
                    resultValues.put(headerName, numValue.intValue());
                }
            } 

            if (emptyCellInHierarchyPart) {
                continue; 
            }

            if (lastItem != null) {
                if (lastItem.getResult() == null || lastItemAddedToHierarchy) { // Set if new or no results yet
                    lastItem.setResult(resultValues);
                } else {
                     this.parserMessages.add(String.format("Info: Item '%s' (row %d, sheet '%s') already had results. New values for this hierarchy were: %s. Not overwriting.", lastItem.getId(), i + 1, sheetName, resultValues.toString()));
                }
            } else if (!resultValues.isEmpty()) { 
                Item valueItem = new Item();
                String generatedId = "sheet_" + sheetName.replaceAll("[^a-zA-Z0-9]", "") + "_row_" + (i + 1) + "_" + reportId;
                valueItem.setId(StringUtils.abbreviate(generatedId, 100));
                valueItem.setName("Data Row " + (i + 1) + " (Sheet: " + sheetName + ")");
                valueItem.setResult(resultValues);
                report.addItem(valueItem);
                if (colIdxValueStart == 0) { 
                     this.parserMessages.add(String.format("Info: Row %d in sheet '%s' has all columns treated as values. Created item '%s'.", i + 1, sheetName, valueItem.getName()));
                } else { 
                     this.parserMessages.add(String.format("Warning: Row %d in sheet '%s' produced values but no specific hierarchy item was determined. Created generic item '%s'.", i + 1, sheetName, valueItem.getName()));
                }
            }
        } 
        return report;
    }
}
