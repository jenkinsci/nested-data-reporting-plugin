package io.jenkins.plugins.reporter.provider;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.Extension;
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
        return new CsvParser();
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

        @Override
        public ReportDto parse(File file) throws IOException {
            CsvMapper mapper = new CsvMapper();

            MappingIterator<List<String>> it = mapper
                    .readerForListOf(String.class)
                    .with(com.fasterxml.jackson.dataformat.csv.CsvParser.Feature.WRAP_AS_ARRAY)
                    .readValues(file);

            ReportDto report = new ReportDto();
            report.setItems(new ArrayList<>());

            List<String> header = it.next();

            List<List<String>> rows = it.readAll();

            String parentId = "report";

            for (List<String> row : rows) {

                Item last = new Item();
                LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

                for (String value : row) {

                    if (isNumber(value)) {

                        parentId = "report";

                        String id = header.get(row.indexOf(value));
                        int val = Integer.parseInt(value);

                        result.put(id, val);

                    } else {

                        if (StringUtils.isEmpty(value)) {
                            continue;
                        }

                        Optional<Item> parent = report.findItem(parentId, report.getItems());
                        Item item = new Item();
                        item.setId(value);
                        item.setName(value);

                        if (parent.isPresent()) {

                            Item p = parent.get();

                            if (!p.hasItems()) {
                                p.setItems(new ArrayList<>());
                            }

                            if (p.getItems().stream().noneMatch(i -> i.getId().equals(value))) {
                                p.addItem(item);
                            }

                        } else {

                            if (report.getItems().stream().noneMatch(i -> i.getId().equals(value))) {
                                report.getItems().add(item);
                            }

                        }

                        parentId = value;
                        last = item;

                    }

                }

                last.setResult(result);

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
