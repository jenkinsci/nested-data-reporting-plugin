package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
        
        return new CsvParser(getActualId());
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

    public static class CsvParser extends ReportParser {

        private static final long serialVersionUID = -8689695008930386640L;
        
        private final String id;
        
        public CsvParser(String id) {
            super();
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public ReportDto parse(File file) throws IOException {
            
            CsvMapper mapper = new CsvMapper();
            CsvSchema  schema = mapper.schemaFor(String[].class).withColumnSeparator(',');
            MappingIterator<List<String>> it = mapper
                    .readerForListOf(String.class)
                    .with(com.fasterxml.jackson.dataformat.csv.CsvParser.Feature.WRAP_AS_ARRAY)
                    .with(schema)
                    .readValues(file);

            ReportDto report = new ReportDto();
            report.setId(getId());
            report.setItems(new ArrayList<>());

            List<String> header = it.next();
            List<List<String>> rows = it.readAll();

            int rowCount = rows.size();
            int headerColumnCount = header.size();
            int colIdxValueStart = 0;

            /** Parse all data rows */
            for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
                String parentId = "report";
                List<String> row = rows.get(rowIdx);
                Item last = null;
                LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

                /** Parse first data line to get data and value field */
                if ((colIdxValueStart == 0) && (row.size() >= header.size())) {
                    for (int colIdx = 0; colIdx < headerColumnCount; colIdx++) {
                        String value = row.get(colIdx);
                        if (isNumber(value) || StringUtils.isEmpty(value)) {
                            colIdxValueStart = colIdx;
                            break;
                        }
                    }
                }

                /** Parse line if first data line is OK and line has more element than header */
                if ((colIdxValueStart > 0) && (row.size() >= headerColumnCount)) {
                    for (int colIdx = 0; colIdx < headerColumnCount; colIdx++) {
                        String id = header.get(colIdx);
                        String value = row.get(colIdx);

                        /** Check value fields */
                        if ((colIdx < colIdxValueStart)) {
                            String valueId = "=?!__" + colIdx + value;

                            /** Skip text fields not ok */
                            if ((StringUtils.isEmpty(value)) || (isNumber(value))) {
                                //logInfo("Invalid Data row = '%i' col = '%i'", rowIdx + 2, colIdx + 1);
                                continue;
                            }

                            Optional<Item> parent = report.findItem(parentId, report.getItems());
                            Item item = new Item();
                            item.setId(valueId);
                            item.setName(value);

                            if (parent.isPresent()) {
                                Item p = parent.get();
                                if (!p.hasItems()) {
                                    p.setItems(new ArrayList<>());
                                }

                                if (p.getItems().stream().noneMatch(i -> i.getId().equals(valueId))) {
                                    p.addItem(item);
                                }
                            } else {
                                if (report.getItems().stream().noneMatch(i -> i.getId().equals(valueId))) {
                                    report.getItems().add(item);
                                }
                            }
                            parentId = valueId;
                            last = item;
                        } else {
                            int val = 0;
                            if (isNumber(value)) {
                                val = Integer.parseInt(value);
                            }
                            result.put(id, val);
                        }
                    }
                } else {
                    /** Skip file if first data line has no value field */
                    if (colIdxValueStart == 0) {
                        //logInfo("No text field found in first data row -  row = '%i'", rowIdx + 2);
                        break;
                    } else {
                        //logInfo("Csv line '%i' has fewer element than title", rowIdx + 2);
                    }
                }
                if (last != null) {
                    //logInfo("Empty line found row = '%i'", rowIdx + 2);
                    last.setResult(result);
                }
            }
            return report;
        }

        private boolean isNumber(String value) {
            try {
                Integer.parseInt(value);
                return true;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
    }
}
