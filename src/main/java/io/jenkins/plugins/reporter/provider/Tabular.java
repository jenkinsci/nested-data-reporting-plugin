package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public abstract class Tabular extends Provider {

    private static final long serialVersionUID = 2427895324546453L;

    public static abstract class TabularParser extends ReportParser {

        private static final long serialVersionUID = -8689695008930386640L;

        private final String id;

        private List<String> parserMessages;

        public TabularParser(String id) {
            super();
            this.id = id;
            this.parserMessages = new ArrayList<String>();
        }

        public String getId() {
            return id;
        }

        public ReportDto parse(List<String> header, List<List<String>> rows) {
            return parse(header, rows, null);
        }

        public ReportDto parse(List<String> header, List<List<String>> rows, List<List<Integer>> cellTypes) {
            ReportDto report = new ReportDto();
            report.setId(getId());
            report.setItems(new ArrayList<>());

            int rowCount = 0;
            final int headerColumnCount = header.size();

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
                int colIdxValueStart = 0;

                /** Parse untill first data line is found to get data and value field */
                if (colIdxValueStart == 0) {
                    /** Col 0 is assumed to be string */
                    for (int colIdx = rowSize - 1; colIdx >= 0; colIdx--) {
                        if (cellTypes != null && cellTypes.get(rowIdx + 1).get(colIdx) == 0) {
                            colIdxValueStart = colIdx;
                        } else if (colIdxValueStart > 0) {
                            break;
                        }
                    }
                }

                String valueId = "";
                /** Parse line if first data line is OK and line has more element than header */
                if ((colIdxValueStart > 0) && (rowSize >= headerColumnCount)) {
                    /** Check line and header size matching */
                    for (int colIdx = 0; colIdx < headerColumnCount; colIdx++) {
                        String id = header.get(colIdx);
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
                            result.put(id, val.intValue());
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
}
