package io.jenkins.plugins.reporter.model;

import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.reporter.ColorProvider;
import io.jenkins.plugins.reporter.ItemViewModel;
import org.apache.commons.text.CaseUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides the model for the item table. The model displays the distribution for the subitems and the id column is 
 * linked to the {@link ItemViewModel} of the selected subitem.
 *
 * @author Simon Symhoven
 */
public class ItemTableModel {
    
    private final Item item;
    private final ColorProvider colorProvider;

    /**
     * Creates a new instance of {@link ItemTableModel}.
     * 
     * @param item
     *         the item to render
     * @param colorProvider
     *         the color mapping for the result.
     */
    public ItemTableModel(Item item, ColorProvider colorProvider) {
        super();
        
        this.item = item;
        this.colorProvider = colorProvider;
    }
    
    public String getId() {
        return item.getId();
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
            .map(item -> new ItemRow(item, colorProvider))
            .collect(Collectors.toList());
    }

    protected TableColumn createResultAbsoluteColumn(String property) {
        return new TableColumn.ColumnBuilder()
                .withDataPropertyKey(String.format("%s-absolute", property))
                .withHeaderLabel(CaseUtils.toCamelCase(property, true))
                .withHeaderClass(TableColumn.ColumnCss.NUMBER)
                .build();
    }

    /**
     * A table row that shows the properties of an item.
     */
    public static class ItemRow {
        
        private final Item item;
        private final ColorProvider colorProvider;

        /**
         * Creates a new instance of {@link ItemRow}.
         * 
         * @param item
         *          the item to render.
         * @param colorProvider
         *          the color mapping for the result of the item.
         */
        ItemRow(Item item, ColorProvider colorProvider) {
            this.item = item;
            this.colorProvider = colorProvider;
        }
        
        public String getId() {
            return item.getId();
        }
        
        public String getName() {
            return item.getName();
        }
        
        public Item getItem() {
            return item;
        }
        
        public ColorProvider getColorProvider() {
            return colorProvider;
        }
        
        public String tooltip(String id, double percentage) {
            return String.format("%s: %.2f%%", id, percentage);
        }
    }
}
