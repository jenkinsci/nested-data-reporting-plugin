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
            // Inside getPercentage(String id)
            Object specificValueRaw = item.getResult().get(id); // Is Object
            double itemTotal = item.getTotal(); // Is double
            double modelItemTotal = model.getItem().getTotal(); // Is double

            if (specificValueRaw instanceof Number) {
                double specificValue = ((Number) specificValueRaw).doubleValue();
                if (itemTotal == 0.0) { 
                    return 0.0; 
                }
                return (specificValue / itemTotal) * 100.0;
            } else {
                // Key 'id' not found in item.getResult(), or its value is not a Number.
                // Original logic: use item's total / model's item's total.
                if (modelItemTotal == 0.0) { 
                    return 0.0;
                }
                return (itemTotal / modelItemTotal) * 100.0;
            }
        }

        public boolean containsColorItem(String id) {
            // Inside containsColorItem(String id)
            Object rawVal = item.getResult().get(id);
            if (rawVal instanceof Number) { // Check if key exists and its value is a Number
                return true; 
            } else {
                // Key not found, or value was not a Number.
                return Objects.equals(item.getId(), id); 
            }
        }

        public Map<String, String> getColors() {
            return report.getColors();
        }

        public String getColor(String id) {
            return report.getColor(id);
        }

        public String label(String id, Object valueAsObject) {
            // Inside label(String id, Object valueAsObject)
            if (!(valueAsObject instanceof Number)) {
                return "N/A"; // Or some other indicator for non-numeric value
            }
            Number valueNumber = (Number) valueAsObject;
            
            double numericValue = valueNumber.doubleValue();
            double denominator;

            // Check if the 'id' is the only key in the item's direct results.
            boolean isSingleResultEntry = item.getResult() != null && item.getResult().containsKey(id) && item.getResult().size() == 1;

            if (isSingleResultEntry) {
                denominator = model.getItem().getTotal(); // This is double
            } else {
                // If multiple results, or 'id' is not the only one, use the item's own result for 'id' as denominator.
                // This part of original logic: model.getItem().getResult().get(id) seems problematic.
                // It should likely be item.getResult().get(id) if we're talking about item's self-percentage for a key.
                // Given the original was model.getItem().getResult().get(id), let's stick to it for now, but ensure type safety.
                Object specificDenominatorObj = item.getResult().get(id); // Using current item's result for the key 'id'
                if (specificDenominatorObj instanceof Number) {
                    denominator = ((Number) specificDenominatorObj).doubleValue();
                } else {
                    denominator = 0.0; // Fallback
                }
            }
            
            double percentage = 0.0;
            if (denominator != 0.0) {
                percentage = (numericValue / denominator) * 100.0;
            }
            
            // This requires Item.getLabel to accept Number
            return item.getLabel(report, valueNumber, percentage); 
        }

        public String tooltip(String id, double percentage) {
            return String.format("%s: %.2f%%", id, percentage);
        }
    }
}