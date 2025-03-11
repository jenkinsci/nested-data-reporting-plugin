package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Csv extends Provider {

    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "csv";

    @DataBoundConstructor
    public Csv() {
        super();
        // empty constructor required for stapler
    }

    @Override
    public ReportParser createParser() {
        if (getActualId().equals(getDescriptor().getId())) {
            throw new IllegalArgumentException(Messages.Provider_Error());
        }

        return new CsvCustomParser(getActualId());
    }

    /** Descriptor for this provider. */
    @Symbol("csv")
    @Extension
    public static class Descriptor extends Provider.ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }

    public static class CsvCustomParser extends ReportParser {

        private static final long serialVersionUID = -8689695008930386640L;

        private final String id;

        private List<String> parserMessages;

        public CsvCustomParser(String id) {
            super();
            this.id = id;
            this.parserMessages = new ArrayList<String>();
        }

        public String getId() {
            return id;
        }

        
        private char detectDelimiter(File file) throws IOException {
            // List of possible delimiters
            char[] delimiters = { ',', ';', '\t', '|' };
            int[] delimiterCounts = new int[delimiters.length];
        
            // Read the lines of the file to detect the delimiter
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                int linesToCheck = 5; // Number of lines to check
                int linesChecked = 0;
        
                String line;
                while ((line = reader.readLine()) != null && linesChecked < linesToCheck) {
                    for (int i = 0; i < delimiters.length; i++) {
                        delimiterCounts[i] += StringUtils.countMatches(line, delimiters[i]);
                    }
                    linesChecked++;
                }
            }
        
            // Return the most frequent delimiter
            int maxCount = 0;
            char detectedDelimiter = 0;
            for (int i = 0; i < delimiters.length; i++) {
                if (delimiterCounts[i] > maxCount) {
                    maxCount = delimiterCounts[i];
                    detectedDelimiter = delimiters[i];
                }
            }
        
            return detectedDelimiter;
        }
        

        @Override
        public ReportDto parse(File file) throws IOException {
            // Get delimiter
            char delimiter = detectDelimiter(file);

            final CsvMapper mapper = new CsvMapper();
            final CsvSchema schema = mapper.schemaFor(String[].class).withColumnSeparator(delimiter);

            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            mapper.enable(CsvParser.Feature.SKIP_EMPTY_LINES);
            mapper.enable(CsvParser.Feature.ALLOW_TRAILING_COMMA);
            mapper.enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
            mapper.enable(CsvParser.Feature.TRIM_SPACES);

            final MappingIterator<List<String>> it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(file);

            ReportDto report = new ReportDto();
            report.setId(getId());
            report.setItems(new ArrayList<>());

            final List<String> header = it.next();
            final List<List<String>> rows = it.readAll();

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

                /** Parse untill first data line is found to get data and value field */
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