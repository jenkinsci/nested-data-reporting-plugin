package io.jenkins.plugins.reporter.util;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for tabular data processing
 * Combines data storage and processing functionality
 */
public class TabularData implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sourceId;
    private final List<String> header;
    private final List<List<String>> rows;

    /** Constructor */
    public TabularData(String sourceId, List<String> header, List<List<String>> rows) {
        this.sourceId = sourceId;
        this.header = header;
        this.rows = rows;
    }

    /** Returns the list of column headers */
    public List<String> getHeader() {
        return header;
    }

    /** Returns the list of data rows */
    public List<List<String>> getRows() {
        List<List<String>> rowsCopy = new ArrayList<>();
        for (List<String> row : rows) {
            rowsCopy.add(new ArrayList<>(row));
        }
        return rowsCopy;
    }
    
    /**
     * Process tabular data to create a ReportDto
     * @param parserMessages List to store parser messages
     * @return ReportDto containing the processed data
     */
    public ReportDto processData(List<String> parserMessages) {
        // Create the report
        ReportDto report = new ReportDto();
        report.setId(sourceId);
        report.setItems(new ArrayList<>());

        int rowCount = 0;
        final int headerColumnCount = header.size();
        
        // First two columns are always category columns, rest are value columns
        final int categoryColumns = 2;
        final int valueColumns = headerColumnCount - categoryColumns;

        if (headerColumnCount >= 2) {
            rowCount = rows.size();
            parserMessages.add(String.format("Processing data with %d rows, %d category columns, %d value columns", 
                rowCount, categoryColumns, valueColumns));
            parserMessages.add(String.format("Headers: %s", String.join(", ", header)));
        } else {
            parserMessages.add(String.format("skipped file - First line has %d elements", headerColumnCount));
            return report;
        }

        /** Parse all data rows */
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            List<String> row = rows.get(rowIdx);
            
            // Add debug info for first and last row
            if (rowIdx == 0 || rowIdx == rows.size() - 1) {  // Only log first and last row for brevity
                parserMessages.add(String.format("Processing row %d: %s", 
                    rowIdx + 1, 
                    String.join(", ", row.subList(0, Math.min(row.size(), 5))) + 
                    (row.size() > 5 ? "..." : "")));
            }
            
            if (row.size() < headerColumnCount) {
                parserMessages.add(String.format("skipped line %d - line has fewer elements than header", rowIdx + 2));
                continue;
            }

            // Process category columns
            String[] categories = new String[categoryColumns];
            boolean validRow = true;
            for (int i = 0; i < categoryColumns; i++) {
                categories[i] = row.get(i).trim();
                if (categories[i].isEmpty()) {
                    validRow = false;
                    break;
                }
            }
            
            if (!validRow) {
                parserMessages.add(String.format("skipped line %d - empty category", rowIdx + 2));
                continue;
            }

            // Process each category level
            Item currentItem = null;
            String parentId = "report";
            
            for (int level = 0; level < categories.length; level++) {
                String categoryName = categories[level];
                String categoryId = level == 0 ? categoryName : parentId + categoryName;
                
                if (level == 0) {
                    // Find or create root level item
                    Optional<Item> existing = report.getItems().stream()
                        .filter(i -> i.getName().equals(categoryName))
                        .findFirst();
                    
                    if (existing.isPresent()) {
                        currentItem = existing.get();
                    } else {
                        currentItem = new Item();
                        currentItem.setId(categoryId);
                        currentItem.setName(categoryName);
                        report.getItems().add(currentItem);
                        parserMessages.add(String.format("Created new root item: %s", categoryName));
                    }
                } else {
                    // Find or create sub-item
                    if (!currentItem.hasItems()) {
                        currentItem.setItems(new ArrayList<>());
                    }
                    
                    Optional<Item> existing = currentItem.getItems().stream()
                        .filter(i -> i.getName().equals(categoryName))
                        .findFirst();
                    
                    if (existing.isPresent()) {
                        currentItem = existing.get();
                    } else {
                        Item newItem = new Item();
                        newItem.setId(categoryId);
                        newItem.setName(categoryName);
                        currentItem.getItems().add(newItem);
                        currentItem = newItem;
                        parserMessages.add(String.format("Created new sub-item: %s under %s", 
                            categoryName, categories[level-1]));
                    }
                }
                parentId = categoryId;
            }

            // Process value columns
            if (currentItem != null) {
                LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
                for (int i = categoryColumns; i < headerColumnCount && i < row.size(); i++) {
                    String headerName = header.get(i);
                    String value = row.get(i);
                    int numericValue = NumberUtils.isCreatable(value) ? 
                        NumberUtils.createNumber(value).intValue() : 0;
                    values.put(headerName, numericValue);
                }

                if (currentItem.getResult() == null) {
                    currentItem.setResult(values);
                } else {
                    // Merge values with existing results
                    LinkedHashMap<String, Integer> existing = currentItem.getResult();
                    for (Map.Entry<String, Integer> entry : values.entrySet()) {
                        existing.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }
                
                parserMessages.add(String.format("Processed %s: %s", currentItem.getName(), 
                    values.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(", "))));
            }
        }
        
        // Add debug info about final report structure
        parserMessages.add(String.format("Final report contains %d root items", report.getItems().size()));
        for (Item item : report.getItems()) {
            parserMessages.add(String.format("Root item: %s with %d subitems", 
                item.getName(),
                item.hasItems() ? item.getItems().size() : 0));
        }
        
        return report;
    }
}
