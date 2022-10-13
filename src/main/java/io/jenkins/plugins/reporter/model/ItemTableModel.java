package io.jenkins.plugins.reporter.model;

import io.jenkins.plugins.datatables.TableColumn;
import org.apache.commons.text.CaseUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Provides the model for the item table. The model displays the distribution for the subitems and the id column is 
 * linked to the {@link io.jenkins.plugins.reporter.ReportDetails} of the selected subitem.
 *
 * @author Simon Symhoven
 */
public class ItemTableModel {

    private final Report report;

    private final Item item;

    /**
     * Creates a new instance of {@link ItemTableModel}.
     *
     * @param report
     *         the report with result
     *
     * @param item
     *         the item to render
     */
    public ItemTableModel(final Report report, final Item item) {
        super();

        this.report = report;
        this.item = item;
    }

    public String getId() {
        return item.getId();
    }

    public Report getReport() {
        return report;
    }

    public Item getItem() {
        return item;
    }

    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();
        item.getResult().keySet().forEach(property -> columns.add(createResultAbsoluteColumn(property)));
        return columns;
    }

    public List<ItemRow> getRows() {
        return item.getItems()
                .stream()
                .map(item -> new ItemRow(report, item, this))
                .collect(Collectors.toList());
    }

    protected TableColumn createResultAbsoluteColumn(String property) {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey(String.format("%s-absolute", property))
                .withHeaderLabel(CaseUtils.toCamelCase(property, true))
                .withHeaderClass(TableColumn.ColumnCss.NUMBER)
                .build();
    }

    public String label(Integer value) {
        return item.getLabel(report, value, value / (double) item.getTotal() * 100);
    }

    /**
     * A table row that shows the properties of an item.
     */
    public static class ItemRow {

        private final Report report;
        private final Item item;

        private final ItemTableModel model;


        /**
         * Creates a new instance of {@link ItemRow}.
         *
         * @param report
         *          the report with the result.
         *
         * @param item
         *          the item to render.
         */
        ItemRow(Report report, Item item, ItemTableModel model) {
            this.report = report;
            this.item = item;
            this.model = model;
        }

        public String getId() {
            return item.getEncodedId();
        }

        public String getName() {
            return item.getName();
        }

        public Item getItem() {
            return item;
        }

        public double getPercentage(String id) {
            int val = item.getResult().getOrDefault(id, -1);

            if (val == -1) {
                val = item.getTotal();

                return val / (double) model.getItem().getTotal() * 100;
            }

            return val / (double) item.getTotal() * 100;
        }

        public boolean containsColorItem(String id) {
            int val = item.getResult().getOrDefault(id, -1);

            if (val == -1) {
                return Objects.equals(item.getId(), id);
            }

            return true;
        }

        public Map<String, String> getColors() {
            return report.getColors();
        }

        public String getColor(String id) {
            return report.getColor(id);
        }

        public String label(String id, Integer value) {
            if (item.getResult().size() == 1) {
                return item.getLabel(report, value, value / (double) model.getItem().getTotal() * 100);
            }

            return item.getLabel(report, value, value / (double) model.getItem().getResult().get(id) * 100);
        }

        public String tooltip(String id, double percentage) {
            return String.format("%s: %.2f%%", id, percentage);
        }
    }
}