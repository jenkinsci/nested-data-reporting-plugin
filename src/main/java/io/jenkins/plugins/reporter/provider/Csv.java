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
import io.jenkins.plugins.reporter.parser.AbstractReportParserBase;
import org.apache.commons.lang3.StringUtils;
// import org.apache.commons.lang3.math.NumberUtils; // Already commented out or removed
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

    public static class CsvCustomParser extends AbstractReportParserBase { // Changed superclass

        private static final long serialVersionUID = -8689695008930386640L; // Keep existing UID for now

        private final String id;

        private List<String> parserMessages; // This will be used by AbstractReportParserBase methods

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
            String[] delimiterNames = { "Comma", "Semicolon", "Tab", "Pipe" };
            int[] delimiterCounts = new int[delimiters.length];
        
            // Read the lines of the file to detect the delimiter
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                int linesToCheck = 10; // Number of lines to check
                int linesChecked = 0;
        
                String line;
                while ((line = reader.readLine()) != null && linesChecked < linesToCheck) {
                    if (StringUtils.isBlank(line)) { // Skip blank lines
                        continue;
                    }
                    for (int i = 0; i < delimiters.length; i++) {
                        delimiterCounts[i] += StringUtils.countMatches(line, delimiters[i]);
                    }
                    linesChecked++;
                }
            }
        
            // Determine the most frequent delimiter
            int maxCount = 0;
            int detectedDelimiterIndex = -1;
            for (int i = 0; i < delimiters.length; i++) {
                if (delimiterCounts[i] > maxCount) {
                    maxCount = delimiterCounts[i];
                    detectedDelimiterIndex = i;
                }
            }
            
            char detectedDelimiter = (detectedDelimiterIndex != -1) ? delimiters[detectedDelimiterIndex] : ','; // Default to comma if none found

            if (detectedDelimiterIndex != -1) {
                // Check for ambiguity
                for (int i = 0; i < delimiters.length; i++) {
                    if (i == detectedDelimiterIndex) continue;
                    // Ambiguous if another delimiter's count is > 0, and difference is less than 20% of max count,
                    // and both counts are above a threshold (e.g., 5)
                    if (delimiterCounts[i] > 5 && maxCount > 5 && 
                        (maxCount - delimiterCounts[i]) < (maxCount * 0.2)) {
                        this.parserMessages.add(String.format(
                            "Warning [CSV]: Ambiguous delimiter. %s count (%d) is very similar to %s count (%d). Using '%c'.",
                            delimiterNames[detectedDelimiterIndex], maxCount,
                            delimiterNames[i], delimiterCounts[i],
                            detectedDelimiter));
                        break; // Log once for the first ambiguity found
                    }
                }
                 this.parserMessages.add(String.format("Info [CSV]: Detected delimiter: '%c' (Name: %s, Count: %d)", 
                    detectedDelimiter, delimiterNames[detectedDelimiterIndex], maxCount));
            } else {
                 this.parserMessages.add("Warning [CSV]: No clear delimiter found. Defaulting to comma ','. Parsing might be inaccurate.");
            }
        
            return detectedDelimiter;
        }
        

        @Override
        public ReportDto parse(File file) throws IOException {
            this.parserMessages.clear(); // Clear messages for each new parse operation
            // Get delimiter
            char delimiter = detectDelimiter(file);

            final CsvMapper mapper = new CsvMapper();
            final CsvSchema schema = mapper.schemaFor(String[].class).withColumnSeparator(delimiter).withoutQuoteChar(); // Try without quote char initially

            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            // mapper.enable(CsvParser.Feature.SKIP_EMPTY_LINES); // We will handle empty line skipping manually for logging
            mapper.disable(CsvParser.Feature.SKIP_EMPTY_LINES);
            mapper.enable(CsvParser.Feature.ALLOW_TRAILING_COMMA);
            mapper.enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
            mapper.enable(CsvParser.Feature.TRIM_SPACES);
            
            ReportDto report = new ReportDto();
            report.setId(getId());
            report.setItems(new ArrayList<>());

            List<String> header = null;
            final int MAX_LINES_TO_SCAN_FOR_HEADER = 20;
            int linesScannedForHeader = 0;
            
            MappingIterator<List<String>> it = null;
            try {
                it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(file);
            } catch (Exception e) {
                 this.parserMessages.add("Error [CSV]: Failed to initialize CSV reader: " + e.getMessage());
                 report.setParserLogMessages(this.parserMessages);
                 return report;
            }


            while (it.hasNext() && linesScannedForHeader < MAX_LINES_TO_SCAN_FOR_HEADER) {
                List<String> currentRow;
                long currentLineNumber = 0;
                try {
                    currentLineNumber = it.getCurrentLocation() != null ? it.getCurrentLocation().getLineNr() : -1;
                    currentRow = it.next();
                } catch (Exception e) {
                    this.parserMessages.add(String.format("Error [CSV]: Could not read line %d: %s", currentLineNumber, e.getMessage()));
                    linesScannedForHeader++; // Count this as a scanned line
                    continue; 
                }

                linesScannedForHeader++;
                if (currentRow == null || currentRow.stream().allMatch(s -> s == null || s.isEmpty())) {
                    this.parserMessages.add(String.format("Info [CSV]: Skipped empty or null line at file line number: %d while searching for header.", currentLineNumber));
                    continue;
                }
                header = currentRow;
                this.parserMessages.add(String.format("Info [CSV]: Using file line %d as header: %s", currentLineNumber, header.toString()));
                break;
            }

            if (header == null) {
                this.parserMessages.add("Error [CSV]: No valid header row found after scanning " + linesScannedForHeader + " lines. Cannot parse file.");
                report.setParserLogMessages(this.parserMessages);
                return report;
            }

            if (header.size() < 2) {
                this.parserMessages.add(String.format("Error [CSV]: Insufficient columns in header (found %d, requires at least 2). Header: %s", header.size(), header.toString()));
                report.setParserLogMessages(this.parserMessages);
                return report;
            }
            
            final List<List<String>> rows = new ArrayList<>();
            long linesReadForData = 0; 
            while(it.hasNext()) { // Collect all data rows first
                linesReadForData++;
                try {
                    List<String> r = it.next();
                    if (r != null) {
                         rows.add(r);
                    } else { 
                        this.parserMessages.add(String.format("Info [CSV]: Encountered a null row object at data line %d, skipping.", linesReadForData));
                    }
                } catch (Exception e) {
                    this.parserMessages.add(String.format("Error [CSV]: Failed to read data row at data line %d: %s. Skipping row.", linesReadForData, e.getMessage()));
                }
            }

            List<String> firstActualDataRow = null;
            for (List<String> r : rows) {
                // Check if row has any non-blank content, considering nulls from INSERT_NULLS_FOR_MISSING_COLUMNS
                if (r.stream().anyMatch(s -> s != null && !s.isEmpty())) { 
                    firstActualDataRow = r;
                    break;
                }
            }
            
            if (firstActualDataRow == null) { // All data rows are empty or no data rows at all
                 if (rows.isEmpty()) {
                    this.parserMessages.add("Info [CSV]: No data rows found after header.");
                 } else {
                    this.parserMessages.add("Info [CSV]: All data rows after header are empty or contain only blank fields. No structure to detect or items to parse.");
                 }
                 report.setParserLogMessages(this.parserMessages);
                 return report;
            }
            
            int colIdxValueStart = detectColumnStructure(header, firstActualDataRow, this.parserMessages, "CSV");
            if (colIdxValueStart == -1) { 
                // Error logged by detectColumnStructure
                report.setParserLogMessages(this.parserMessages); 
                return report; 
            }

            /** Parse all data rows */
            for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
                List<String> row = rows.get(rowIdx);
                // Pass rowIdx as rowIndexForLog, it's 0-based index into the 'rows' list
                parseRowToItems(report, row, header, colIdxValueStart, this.id, this.parserMessages, "CSV", rowIdx);
            }
            
            // Final check if items were added, especially if all rows were skipped by parseRowToItems
            if (report.getItems().isEmpty() && !rows.isEmpty() && 
                !rows.stream().allMatch(r -> r.stream().allMatch(s -> s==null || s.isEmpty())) ) { // if not all rows were completely blank initially
                 this.parserMessages.add("Warning [CSV]: No items were successfully parsed from data rows. Check data integrity and column structure detection logs.");
            }

            report.setParserLogMessages(this.parserMessages);
            return report;
        }
    }
}