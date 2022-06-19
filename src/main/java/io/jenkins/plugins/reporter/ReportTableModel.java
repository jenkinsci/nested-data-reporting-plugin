package io.jenkins.plugins.reporter;

import io.jenkins.plugins.datatables.DetailedCell;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static j2html.TagCreator.s;
import static j2html.TagCreator.span;

public class ReportTableModel extends TableModel {
    
    private static final String REPORT_ID = "report-aggregated-table";

    private final String id;
    private final Report report;
    
    
    public ReportTableModel(String id, Report report) {
        super();
        
        this.id = id;
        this.report = report;
    }
    
    @Override
    public String getId() {
        return id.equals(REPORT_ID) ? REPORT_ID : id;
    }

    @Override
    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();

        
        columns.add(new TableColumn.ColumnBuilder()
                .withDataPropertyKey("id")
                .withHeaderLabel("ID")
                .withHeaderClass(TableColumn.ColumnCss.NONE)
                .build());
        
        columns.add(new TableColumn.ColumnBuilder()
                .withDataPropertyKey("distribution")
                .withHeaderLabel("Distribution")
                .withHeaderClass(TableColumn.ColumnCss.NO_SORT)
                .withDetailedCell()
                .build());
     
        return columns;
    }

    @Override
    public List<Object> getRows() {
        
        if (id.equals(REPORT_ID)) {
            return report.getResult().getComponents()
                    .stream()
                    .map(item -> new TableRow(item, report.getResult().getColors()))
                    .collect(Collectors.toList());
        }
        
        Optional<Item> subItem = report.getResult().getComponents().stream().filter(item -> item.getId().equals(id)).findFirst();

        return subItem.<List<Object>>map(value -> value.getItems()
                .stream()
                .map(item -> new TableRow(item, report.getResult().getColors()))
                .collect(Collectors.toList())).orElseGet(ArrayList::new);

    }
    
    public static class TableRow {
        
        private final Item item;
        private final Map<String, String> colors;
        
        TableRow(Item item, Map<String, String> colors) {
            this.item = item;
            this.colors = colors;
        }
        
        public String getId() {
            return item.getId(); 
        }
        
        public DetailedCell<String> getDistribution() {
            return createColoredResultColumn(item);
        }

        protected DetailedCell<String> createColoredResultColumn(final Item item) {
           String tag = span()
                    .withTitle(item.getResult().values().stream().map(Object::toString).collect(Collectors.joining("/")))
                    .withStyle(String.format("color: transparent; background-image: linear-gradient(to right %s); display:block;", createGradient()))
                    .withText(item.getId())
                    .attr("data-bs-toggle", "tooltip")
                    .attr("data-bs-placement", "left")
                    .render();
            
            return new DetailedCell<String>(tag, null);
        }
        
        protected String createGradient() {
            int total = item.getResult().values().stream().reduce(0, Integer::sum);
            
            StringBuilder builder = new StringBuilder();
            double oldPercentage = 0;

            for (Map.Entry<String, String> color : colors.entrySet()) {
                String id = color.getKey();
                String hex = color.getValue();
                
                int val = item.getResult().get(id);
                double percentage = (val / (double) total) * 100;
                builder.append(String.format(", %s %s%%, %s %s%%", hex, oldPercentage, hex, oldPercentage + percentage));
                oldPercentage += percentage;
            }
            
            return builder.toString();
        }
    }
}
