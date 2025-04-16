package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import io.jenkins.plugins.reporter.util.TabularData;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Provider for CSV files
 */
public class Csv extends Provider {

    private static final long serialVersionUID = 9141170397250309265L;

    private static final String ID = "csv";

    /** Default constructor */
    @DataBoundConstructor
    public Csv() {
        super();
        // empty constructor required for stapler
    }

    /** Creates a CSV parser */
    @Override
    public ReportParser createParser() {
        if (getActualId().equals(getDescriptor().getId())) {
            throw new IllegalArgumentException(Messages.Provider_Error());
        }

        return new CsvCustomParser(getActualId());
    }

    /** Descriptor for this provider */
    @Symbol("csv")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates a descriptor instance */
        public Descriptor() {
            super(ID);
        }
    }

    /**
     * Parser for CSV files
     */
    public static class CsvCustomParser extends ReportParser {

        private static final long serialVersionUID = -8689695008930386640L;

        private final String id;
        private List<String> parserMessages;

        /** Constructor */
        public CsvCustomParser(String id) {
            super();
            this.id = id;
            this.parserMessages = new ArrayList<String>();
        }

        /** Returns the parser identifier */
        public String getId() {
            return id;
        }

        /** Detects the delimiter used in the CSV file */
        private char detectDelimiter(File file) throws IOException {
            // List of possible delimiters
            char[] delimiters = { ',', ';', '\t', '|' };
            int[] delimiterCounts = new int[delimiters.length];
        
            // Read the file lines to detect the delimiter
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

        /** Parses a CSV file and creates a ReportDto */
        @Override
        public ReportDto parse(File file) throws IOException {
            // Delimiter detection
            char delimiter = detectDelimiter(file);

            // CSV parser configuration
            final CsvMapper mapper = new CsvMapper();
            final CsvSchema schema = mapper.schemaFor(String[].class).withColumnSeparator(delimiter);

            mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
            mapper.enable(CsvParser.Feature.SKIP_EMPTY_LINES);
            mapper.enable(CsvParser.Feature.ALLOW_TRAILING_COMMA);
            mapper.enable(CsvParser.Feature.INSERT_NULLS_FOR_MISSING_COLUMNS);
            mapper.enable(CsvParser.Feature.TRIM_SPACES);

            // Read CSV data
            final MappingIterator<List<String>> it = mapper.readerForListOf(String.class)
                    .with(schema)
                    .readValues(file);

            // Extract headers and rows
            final List<String> header = it.next();
            final List<List<String>> rows = it.readAll();

            // Create TabularData object and process it
            TabularData tabularData = new TabularData(id, header, rows);
            return tabularData.processData(parserMessages);
        }
    }
}
