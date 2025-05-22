package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser; // Extends this
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;


public abstract class AbstractReportParserBase extends ReportParser {

    private static final long serialVersionUID = 5738290018231028471L; // New UID
    protected static final Logger PARSER_LOGGER = Logger.getLogger(AbstractReportParserBase.class.getName());

    /**
     * Detects the column structure (hierarchy vs. value columns) of a report.
     *
     * @param header         The list of header strings.
     * @param firstDataRow   A list of string values from the first representative data row.
     * @param messagesCollector A list to collect informational/warning messages.
     * @param parserName     A short name of the parser type (e.g., "CSV", "Excel") for message logging.
     * @return The starting column index for value/numeric data. Returns -1 if structure cannot be determined or is invalid.
     */
    protected int detectColumnStructure(List<String> header, List<String> firstDataRow, List<String> messagesCollector, String parserName) {
        if (header == null || header.isEmpty()) {
            messagesCollector.add(String.format("Warning [%s]: Header is empty, cannot detect column structure.", parserName));
            return -1;
        }
        if (firstDataRow == null || firstDataRow.isEmpty()) {
            messagesCollector.add(String.format("Warning [%s]: First data row is empty, cannot reliably detect column structure.", parserName));
            // Proceed assuming last column is value if header has multiple columns, else ambiguous.
            if (header.size() > 1) {
                messagesCollector.add(String.format("Info [%s]: Defaulting structure: Assuming last column ('%s') for values due to empty first data row.", parserName, header.get(header.size() -1)));
                return header.size() - 1;
            } else if (header.size() == 1) {
                 messagesCollector.add(String.format("Info [%s]: Single column header ('%s') and empty first data row. Structure ambiguous.", parserName, header.get(0)));
                 return 0; // Treat as value column by default
            }
            return -1;
        }

        int determinedColIdxValueStart = 0; 
        for (int cIdx = header.size() - 1; cIdx >= 0; cIdx--) {
            String cellVal = (cIdx < firstDataRow.size()) ? firstDataRow.get(cIdx) : "";
            if (NumberUtils.isCreatable(cellVal)) {
                determinedColIdxValueStart = cIdx;
            } else {
                if (determinedColIdxValueStart > cIdx && determinedColIdxValueStart != 0) { 
                    break; 
                }
            }
        }

        if (determinedColIdxValueStart == 0 && !NumberUtils.isCreatable(firstDataRow.get(0))) { 
            if (header.size() > 1) {
                determinedColIdxValueStart = header.size() - 1; 
                messagesCollector.add(String.format("Warning [%s]: No numeric columns auto-detected. Assuming last column ('%s') for values.", parserName, header.get(determinedColIdxValueStart)));
            } else { 
                messagesCollector.add(String.format("Info [%s]: Single text column ('%s'). No numeric data values expected.", parserName, header.get(0)));
            }
        } else if (determinedColIdxValueStart == 0 && NumberUtils.isCreatable(firstDataRow.get(0))) {
            messagesCollector.add(String.format("Info [%s]: First column ('%s') is numeric. Treating it as the first value column.", parserName, header.get(0)));
        }
        
        messagesCollector.add(String.format("Info [%s]: Detected data structure: Hierarchy/Text columns: 0 to %d, Value/Numeric columns: %d to %d.", 
            parserName, Math.max(0, determinedColIdxValueStart - 1), determinedColIdxValueStart, header.size() - 1));

        if (determinedColIdxValueStart >= header.size() || determinedColIdxValueStart < 0) {
            messagesCollector.add(String.format("Error [%s]: Invalid structure detected (value_start_index %d out of bounds for header size %d).", 
                parserName, determinedColIdxValueStart, header.size()));
            return -1; // Invalid structure
        }
        return determinedColIdxValueStart;
    }

    /**
     * Parses a single row of data and converts it into hierarchical Item objects.
     *
     * @param reportDto             The ReportDto to which items will be added.
     * @param rowValues             The list of string values for the current row.
     * @param header                The list of header strings.
     * @param colIdxValueStart      The starting column index for value data.
     * @param baseItemIdPrefix      A prefix for generating item IDs (e.g., reportId or sheet-specific ID).
     * @param messagesCollector     A list to collect informational/warning messages.
     * @param parserName            A short name of the parser type (e.g., "CSV", "Excel") for message logging.
     */
    protected void parseRowToItems(ReportDto reportDto, List<String> rowValues, List<String> header, 
                                 int colIdxValueStart, String baseItemIdPrefix, 
                                 List<String> messagesCollector, String parserName, int rowIndexForLog) {

        if (rowValues == null || rowValues.isEmpty()) {
            messagesCollector.add(String.format("Info [%s]: Skipped empty row at data index %d.", parserName, rowIndexForLog));
            return;
        }
        
        // If row is shorter than expected hierarchy columns, it might be problematic.
        if (rowValues.size() < colIdxValueStart && colIdxValueStart > 0) {
            messagesCollector.add(String.format("Warning [%s]: Skipped data row at index %d: Row has %d cells, but hierarchy part expects at least %d.", 
                parserName, rowIndexForLog, rowValues.size(), colIdxValueStart));
            return;
        }

        String parentId = "report"; // Special root parent ID for top-level items
        Item lastItem = null;
        boolean lastItemWasNewlyCreated = false;
        LinkedHashMap<String, Integer> resultValuesMap = new LinkedHashMap<>();
        boolean issueInHierarchy = false;
        String currentItemPathId = StringUtils.isNotBlank(baseItemIdPrefix) ? baseItemIdPrefix + "::" : "";

        for (int colIdx = 0; colIdx < header.size(); colIdx++) {
            String headerName = header.get(colIdx);
            String rawCellValue = (colIdx < rowValues.size() && rowValues.get(colIdx) != null) ? rowValues.get(colIdx).trim() : "";

            if (colIdx < colIdxValueStart) { // This column is part of the hierarchy
                String hierarchyCellValue = rawCellValue;
                String originalCellValueForName = rawCellValue;

                if (StringUtils.isBlank(hierarchyCellValue)) {
                    if (colIdx == 0) { // First hierarchy column cannot be blank
                        messagesCollector.add(String.format("Warning [%s]: Skipped data row at index %d: First hierarchy column ('%s') is empty.", 
                            parserName, rowIndexForLog, headerName));
                        issueInHierarchy = true;
                        break; 
                    }
                    messagesCollector.add(String.format("Info [%s]: Data row index %d, Col %d (Header '%s') is part of hierarchy and is blank. Using placeholder ID part.", 
                        parserName, rowIndexForLog, colIdx + 1, headerName));
                    hierarchyCellValue = "blank_hier_" + colIdx; // Use placeholder for ID generation
                } else if (NumberUtils.isCreatable(hierarchyCellValue)) {
                    messagesCollector.add(String.format("Info [%s]: Data row index %d, Col %d (Header '%s') is part of hierarchy but is numeric-like ('%s'). Using as string for ID/Name.", 
                        parserName, rowIndexForLog, colIdx + 1, headerName, hierarchyCellValue));
                }
                // Check if a non-empty hierarchy cell appears after a blank one (if issueInHierarchy was set due to blank)
                // This check is usually done by comparing originalCellValue with a flag set by previous blank cell.
                // For simplicity here, we assume the `break` for colIdx == 0 handles the critical case.

                currentItemPathId += hierarchyCellValue.replaceAll("[^a-zA-Z0-9_-]", "_") + "_";
                String itemId = StringUtils.removeEnd(currentItemPathId, "_");
                if (StringUtils.isBlank(itemId)) { // Should not happen if baseItemIdPrefix is good and placeholders are used
                    itemId = baseItemIdPrefix + "::unnamed_item_r" + rowIndexForLog + "_c" + colIdx;
                }
                
                Optional<Item> parentOpt = reportDto.findItem(parentId, reportDto.getItems());
                Item currentItem = new Item();
                currentItem.setId(StringUtils.abbreviate(itemId, 250)); // Ensure ID is not excessively long
                currentItem.setName(StringUtils.isBlank(originalCellValueForName) ? "(blank)" : originalCellValueForName);
                lastItemWasNewlyCreated = false;

                if (parentOpt.isPresent()) {
                    Item p = parentOpt.get();
                    if (p.getItems() == null) p.setItems(new ArrayList<>());
                    
                    Optional<Item> existingItem = p.getItems().stream().filter(it -> it.getId().equals(currentItem.getId())).findFirst();
                    if (!existingItem.isPresent()) {
                        p.addItem(currentItem);
                        lastItemWasNewlyCreated = true;
                        lastItem = currentItem;
                    } else {
                        lastItem = existingItem.get();
                    }
                } else { // No parent found, this is a top-level item in the current context (under "report")
                    Optional<Item> existingRootItem = reportDto.getItems().stream().filter(it -> it.getId().equals(currentItem.getId())).findFirst();
                    if (!existingRootItem.isPresent()) {
                        if (reportDto.getItems() == null) reportDto.setItems(new ArrayList<>()); // Defensive check
                        reportDto.getItems().add(currentItem);
                        lastItemWasNewlyCreated = true;
                        lastItem = currentItem;
                    } else {
                        lastItem = existingRootItem.get();
                    }
                }
                parentId = currentItem.getId(); // For the next level of hierarchy

            } else { // This column is part of the values
                Number numValue = 0;
                if (NumberUtils.isCreatable(rawCellValue)) {
                    numValue = NumberUtils.createNumber(rawCellValue);
                } else if (StringUtils.isNotBlank(rawCellValue)) {
                    messagesCollector.add(String.format("Warning [%s]: Non-numeric value '%s' in data column '%s' at data row index %d, col %d. Using 0.", 
                        parserName, rawCellValue, headerName, rowIndexForLog, colIdx + 1));
                }
                resultValuesMap.put(headerName, numValue.intValue());
            }
        } // End column loop

        if (issueInHierarchy) {
            return; // Row processing was aborted
        }

        if (lastItem != null) { // A hierarchy item was identified or created for this row
            if (lastItem.getResult() == null || lastItemWasNewlyCreated) {
                lastItem.setResult(resultValuesMap);
            } else {
                // Item existed and had results. Decide on merging or warning.
                // For now, log and don't overwrite unless explicitly designed for merging.
                messagesCollector.add(String.format("Info [%s]: Item '%s' (data row index %d) already had results. New values for this row were: %s. Not overwriting existing results.", 
                    parserName, lastItem.getId(), rowIndexForLog, resultValuesMap.toString()));
            }
        } else if (!resultValuesMap.isEmpty()) { // No hierarchy columns (colIdxValueStart == 0), but values exist
            Item valueItem = new Item();
            String generatedId = (StringUtils.isNotBlank(baseItemIdPrefix) ? baseItemIdPrefix + "::" : "") + "DataRow_" + rowIndexForLog;
            valueItem.setId(StringUtils.abbreviate(generatedId.replaceAll("[^a-zA-Z0-9_.-]", "_"), 100));
            valueItem.setName("Data Row " + (rowIndexForLog + 1)); // User-friendly name
            valueItem.setResult(resultValuesMap);
            if (reportDto.getItems() == null) reportDto.setItems(new ArrayList<>()); // Defensive check
            reportDto.getItems().add(valueItem);
            messagesCollector.add(String.format("Info [%s]: Data row index %d created as a direct data item '%s' as no distinct hierarchy path was formed (or colIdxValueStart was 0).", 
                parserName, rowIndexForLog, valueItem.getName()));
        } else if (lastItem == null && resultValuesMap.isEmpty() && header.size() > 0) {
            // This means the row was processed, no hierarchy item was relevant (e.g. all blank hierarchy cells not at start), and no values.
            messagesCollector.add(String.format("Warning [%s]: Data row index %d did not yield any identifiable hierarchy item or data values. It might be effectively empty or malformed relative to header.",
                parserName, rowIndexForLog));
        }
    }
}
