package io.jenkins.plugins.reporter.provider;

import hudson.Extension;
import io.jenkins.plugins.reporter.Messages;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ExcelMulti extends Tabular {

    private static final long serialVersionUID = 1L;

    private static final String ID = "excelMulti";

    @DataBoundConstructor
    public ExcelMulti() {
        super();
        // empty constructor required for stapler
    }

    @Override
    public ReportParser createParser() {
        return new ExcelMultiParser(getActualId());
    }

    /** Descriptor for this provider. */
    @Symbol("excelMulti")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }

    public static class ExcelMultiParser extends Tabular.TabularParser {

        private static final long serialVersionUID = 1L;

        public ExcelMultiParser(String id) {
            super(id);
        }

        @Override
        public ReportDto parse(File file) throws IOException {
            try (Workbook workbook = WorkbookFactory.create(file)) {
                List<List<String>> allRows = new ArrayList<>();
                List<String> header = null;

                for (Sheet sheet : workbook) {
                    List<List<String>> sheetData = new ArrayList<>();
                    for (Row row : sheet) {
                        List<String> rowData = new ArrayList<>();
                        for (Cell cell : row) {
                            switch (cell.getCellType()) {
                                case STRING:
                                    rowData.add(cell.getRichStringCellValue().getString());
                                    break;
                                case NUMERIC:
                                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                        rowData.add(cell.getDateCellValue().toString());
                                    } else {
                                        rowData.add(String.valueOf(cell.getNumericCellValue()));
                                    }
                                    break;
                                case BOOLEAN:
                                    rowData.add(String.valueOf(cell.getBooleanCellValue()));
                                    break;
                                case FORMULA:
                                    rowData.add(cell.getCellFormula());
                                    break;
                                default:
                                    rowData.add(null);
                            }
                        }
                        sheetData.add(rowData);
                    }

                    if (!sheetData.isEmpty()) {
                        if (header == null) {
                            header = sheetData.get(0);
                            allRows.addAll(sheetData.subList(1, sheetData.size()));
                        } else {
                            List<String> currentHeader = sheetData.get(0);
                            if (!header.equals(currentHeader)) {
                                throw new IOException("Headers are not consistent across sheets");
                            }
                            allRows.addAll(sheetData.subList(1, sheetData.size()));
                        }
                    }
                }

                if (header == null) {
                    return new ReportDto();
                }

                return parse(header, allRows);
            }
        }
    }
}
