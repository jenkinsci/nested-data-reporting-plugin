package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import io.jenkins.plugins.datatables.DetailedCell;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.prism.Sanitizer;
import io.jenkins.plugins.reporter.ItemViewModel;
import org.apache.commons.text.CaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static j2html.TagCreator.i;
import static j2html.TagCreator.span;

/**
 * Provides the model for the item table. The model displays the distribution for the subitems and the id column is 
 * linked to the {@link ItemViewModel} of the selected subitem.
 *
 * @author Simon Symhoven
 */
public class ItemTableModel extends TableModel {
    
    private final Item item;
    private final Map<String, String> colors;

    /**
     * Creates a new instance of {@link ItemTableModel}.
     * 
     * @param item
     *         the item to render
     * @param colors
     *         the color mapping for the result.
     */
    public ItemTableModel(Item item, Map<String, String> colors) {
        super();
        
        this.item = item;
        this.colors = colors;
    }
    
    @Override
    public String getId() {
        return item.getId();
    }

    @Override
    public List<TableColumn> getColumns() {
        List<TableColumn> columns = new ArrayList<>();
        
        columns.add(createIdColumn());
        columns.add(createNameColumn());

        item.getResult().keySet().forEach(property -> columns.add(createResultColumn(property)));
        
        columns.add(createDistributionColumn());
     
        return columns;
    }

    @Override
    public List<Object> getRows() {
        return item.getItems()
            .stream()
            .map(item -> new ItemRow(item, colors))
            .collect(Collectors.toList());
    }

    protected TableColumn createIdColumn() {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey("id")
                .withHeaderLabel("ID")
                .withHeaderClass(TableColumn.ColumnCss.HIDDEN)
                .build();
    }
    
    protected TableColumn createNameColumn() {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey("name")
                .withHeaderLabel("Name")
                .withHeaderClass(TableColumn.ColumnCss.NONE)
                .build();
    }

    protected TableColumn createResultColumn(String property) {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey(property)
                .withHeaderLabel(CaseUtils.toCamelCase(property, true))
                .withHeaderClass(TableColumn.ColumnCss.NUMBER)
                .build();
    }

    protected TableColumn createDistributionColumn() {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey("distribution")
                .withHeaderLabel("Distribution")
                .withHeaderClass(TableColumn.ColumnCss.NO_SORT)
                .withDetailedCell()
                .build();
    }
    
    /**
     * A table row that shows the properties of an item.
     */
    public static class ItemRow {
        
        private static final Sanitizer SANITIZER = new Sanitizer();
       
        private final Item item;
        private final Map<String, String> colors;

        /**
         * Creates a new instance of {@link ItemRow}.
         * 
         * @param item
         *          the item to render.
         * @param colors
         *          the color mapping for the result of the item.
         */
        ItemRow(Item item, Map<String, String> colors) {
            this.item = item;
            this.colors = colors;
        }
        
        public String getId() {
            return item.getId();
        }
        
        public String getName() {
            return formatProperty(item.getId(), item.getName());
        }
        
        public DetailedCell<String> getDistribution() {
            return createColoredResultColumn(item);
        }

        /**
         * Used to get a Getter for each property of the dynamic result of the item.
         * 
         * @return the result.
         */
        @JsonAnyGetter
        public Map<String, Integer> getResult() {
            return item.getResult();
        }
        
        protected DetailedCell<String> createColoredResultColumn(final Item item) {
           String tag = span()
                    .withTitle(item.getResult().values().stream().map(Object::toString).collect(Collectors.joining("/")))
                    .withStyle(String.format("color: transparent; background-image: linear-gradient(to right %s); display:block;", createGradient()))
                    .withText(item.getId())
                    .attr("data-bs-toggle", "tooltip")
                    .attr("data-bs-placement", "left")
                    .render();
            
            return new DetailedCell<>(tag, null);
        }

        /**
         * Creates the gradient for the distribution for each subitem of item to display in the table cell.
         * 
         * @return the html string with gradient.
         */
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

        /**
         * Formats the text of the specified property column. The text actually is a link to the UI representation of
         * the property.
         *
         * @param link
         *         the property to use as link
         * @param value
         *         the value of the property to be shown
         *
         * @return the formatted column
         */
        protected String formatProperty(final String link, final String value) {
            return String.format("<a href=\"%d/\">%s</a>", link.hashCode(), render(value));
        }

        /**
         * Renders the specified HTML code. Removes unsafe HTML constructs.
         *
         * @param html
         *         the HTML to render
         *
         * @return safe HTML
         */
        protected final String render(final String html) {
            return SANITIZER.render(html);
        }
    }
}
