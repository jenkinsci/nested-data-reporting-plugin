package io.jenkins.plugins.reporter.model;

import hudson.model.Run;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.reporter.ReportAction;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final Run<?, ?> owner;

    /**
     * Creates a new instance of {@link ItemTableModel}.
     *
     * @param report
     *         the report with result
     *
     * @param item
     *         the item to render
     */
    public ItemTableModel(final Report report, final Item item, final Run<?,?> owner) {
        super();

        this.report = report;
        this.item = item;
        this.owner = owner;
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
                .map(item -> new ItemRow(report, item, this, owner))
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
        private final Run<?, ?> owner;

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
        ItemRow(Report report, Item item, ItemTableModel model, Run<?, ?> owner) {
            this.report = report;
            this.item = item;
            this.model = model;
            this.owner = owner;
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
            String label = "";
            if (report.getDisplayType() == DisplayType.ABSOLUTE_AND_DELTA) {
                String delta = getDelta(id);
                if (StringUtils.isNotEmpty(delta)) {
                    label = String.format("%d (%s)", value, delta);
                } else {
                    label = String.valueOf(value);
                }
            } else {
                 if (item.getResult().size() == 1) {
                    label = item.getLabel(report, value, value / (double) model.getItem().getTotal() * 100);
                } else {
                    label = item.getLabel(report, value, value / (double) model.getItem().getResult().get(id) * 100);
                }
            }

            return label;
        }

        public String tooltip(String id, double percentage) {
            return String.format("%s: %.2f%%", id, percentage);
        }

        public String getDelta(String property) {
            Optional<Report> referenceReport = getReferenceReport();
            if (referenceReport.isPresent()) {
                Optional<Item> referenceItem = referenceReport.get().findItem(item.getId());
                if (referenceItem.isPresent()) {

                    int current = item.getResult().get(property);
                    int old = referenceItem.get().getResult().get(property);
                    int delta = current - old;

                    if (delta > 0) {
                        return String.format("+%d", delta);
                    }

                    return String.valueOf(delta);
                }
            }

            return "";
        }

        private Optional<Report> getReferenceReport() {
            Run<?, ?> lastSuccessfulBuild = owner.getParent().getLastSuccessfulBuild();
            if (lastSuccessfulBuild != null) {
                ReportAction action = lastSuccessfulBuild.getAction(ReportAction.class);
                if (action != null) {
                    return Optional.of(action.getReport());
                }
            }

            return Optional.empty();
        }
    }
}