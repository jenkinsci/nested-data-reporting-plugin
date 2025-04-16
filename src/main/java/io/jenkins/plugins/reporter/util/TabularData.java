package io.jenkins.plugins.reporter.util;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.Serializable;
import java.util.*;

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
        int colIdxValueStart = 0;

        if (headerColumnCount >= 2) {
            rowCount = rows.size();
        } else {
            parserMessages.add(String.format("skipped file - First line has %d elements", headerColumnCount + 1));
        }

        /** Parse all data rows */
        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            String parentId = "report";
            List<String> row = rows.get(rowIdx);
            Item last = null;
            boolean lastItemAdded = false;
            LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
            boolean emptyFieldFound = false;
            int rowSize = row.size();

            /** Parse until first data line is found to get data and value field */
            if (colIdxValueStart == 0) {
                /** Col 0 is assumed to be string */
                for (int colIdx = rowSize - 1; colIdx > 1; colIdx--) {
                    String value = row.get(colIdx);

                    if (NumberUtils.isCreatable(value)) {
                        colIdxValueStart = colIdx;
                    } else {
                        if (colIdxValueStart > 0) {
                            parserMessages
                                    .add(String.format("Found data - fields number = %d  - numeric fields = %d",
                                            colIdxValueStart, rowSize - colIdxValueStart));
                        }
                        break;
                    }
                }
            }

            String valueId = "";
            /** Parse line if first data line is OK and line has more element than header */
            if ((colIdxValueStart > 0) && (rowSize >= headerColumnCount)) {
                /** Check line and header size matching */
                for (int colIdx = 0; colIdx < headerColumnCount; colIdx++) {
                    String colId = header.get(colIdx);
                    String value = row.get(colIdx);

                    /** Check value fields */
                    if ((colIdx < colIdxValueStart)) {
                        /** Test if text item is a value or empty */
                        if ((NumberUtils.isCreatable(value)) || (StringUtils.isBlank(value))) {
                            /** Empty field found - message */
                            if (colIdx == 0) {
                                parserMessages
                                        .add(String.format("skipped line %d - First column item empty - col = %d ",
                                                rowIdx + 2, colIdx + 1));
                                break;
                            } else {
                                emptyFieldFound = true;
                                /** Continue next column parsing */
                                continue;
                            }
                        } else {
                            /** Check if field values are present after empty cells */
                            if (emptyFieldFound) {
                                parserMessages.add(String.format("skipped line %d Empty field in col = %d ",
                                        rowIdx + 2, colIdx + 1));
                                break;
                            }
                        }
                        valueId += value;
                        Optional<Item> parent = report.findItem(parentId, report.getItems());
                        Item item = new Item();
                        lastItemAdded = false;
                        item.setId(valueId);
                        item.setName(value);
                        String finalValueId = valueId;
                        if (parent.isPresent()) {
                            Item p = parent.get();
                            if (!p.hasItems()) {
                                p.setItems(new ArrayList<>());
                            }
                            if (p.getItems().stream().noneMatch(i -> i.getId().equals(finalValueId))) {
                                p.addItem(item);
                                lastItemAdded = true;
                            }
                        } else {
                            if (report.getItems().stream().noneMatch(i -> i.getId().equals(finalValueId))) {
                                report.getItems().add(item);
                                lastItemAdded = true;
                            }
                        }
                        parentId = valueId;
                        last = item;
                    } else {
                        Number val = 0;
                        if (NumberUtils.isCreatable(value)) {
                            val = NumberUtils.createNumber(value);
                        }
                        result.put(colId, val.intValue());
                    }
                }
            } else {
                /** Skip file if first data line has no value field */
                if (colIdxValueStart == 0) {
                    parserMessages.add(String.format("skipped line %d - First data row not found", rowIdx + 2));
                    continue;
                } else {
                    parserMessages
                            .add(String.format("skipped line %d - line has fewer element than title", rowIdx + 2));
                    continue;
                }
            }
            /** If last item was created, it will be added to report */
            if (lastItemAdded) {
                last.setResult(result);
            } else {
                parserMessages.add(String.format("ignored line %d - Same fields already exists", rowIdx + 2));
            }
        }
        // report.setParserLog(parserMessages);
        return report;
    }
}
