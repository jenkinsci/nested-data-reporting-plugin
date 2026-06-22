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

public class Excel extends Tabular {

    private static final long serialVersionUID = 1L;

    private static final String ID = "excel";

    @DataBoundConstructor
    public Excel() {
        super();
        // empty constructor required for stapler
    }

    @Override
    public ReportParser createParser() {
        return new ExcelParser(getActualId());
    }

    /** Descriptor for this provider. */
    @Symbol("excel")
    @Extension
    public static class Descriptor extends ProviderDescriptor {
        /** Creates the descriptor instance. */
        public Descriptor() {
            super(ID);
        }
    }

    public static class ExcelParser extends Tabular.TabularParser {

        private static final long serialVersionUID = 1L;

        public ExcelParser(String id) {
            super(id);
        }

        @Override
        public ReportDto parse(File file) throws IOException {
            try (Workbook workbook = WorkbookFactory.create(file)) {
                Sheet sheet = workbook.getSheetAt(0);
                List<List<String>> data = new ArrayList<>();
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
                    data.add(rowData);
                }

                if (data.isEmpty()) {
                    return new ReportDto();
                }

                List<String> header = data.get(0);
                List<List<String>> rows = data.subList(1, data.size());
                List<List<Integer>> cellTypes = new ArrayList<>();

                for (Row row : sheet) {
                    List<Integer> rowCellTypes = new ArrayList<>();
                    for (Cell cell : row) {
                        rowCellTypes.add(cell.getCellType().getCode());
                    }
                    cellTypes.add(rowCellTypes);
                }

                return parse(header, rows, cellTypes);
            }
        }
    }
}
