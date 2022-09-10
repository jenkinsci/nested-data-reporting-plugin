package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds one x-axis point for the series of a line chart 
 * showing the results of an item from the json model.
 *
 * @author Simon Symhoven
 */
public class ItemSeriesBuilder extends SeriesBuilder<ReportAction> {
    
    private final Item item;

    /**
     * Creates a new {@link ItemSeriesBuilder}.
     * 
     * @param item
     *          the item to build the series for.
     */
    public ItemSeriesBuilder(Item item) {
        this.item = item;
    }

    @Override
    protected Map<String, Integer> computeSeries(ReportAction reportAction) {

        if (item.getId().equals(ReportAction.REPORT_ID)) {
            
            if (item.getResult().size() == 1) {
                return reportAction.getReport().getResult().getItems().stream()
                        .collect(Collectors.toMap(Item::getId, Item::getTotal));
            }
            
            return reportAction.getReport().getResult().aggregate();
        }
        
        List<Item> items = findItems(item.getId(), reportAction.getReport().getResult().getItems());
        
        if (item.getResult().size() == 1) {
            return items.stream().collect(Collectors.toMap(Item::getId, Item::getTotal));
        }
        
        return reportAction.getReport().getResult().aggregate(items);
    }

    private List<Item> findItems(String id, List<Item> items)
    {
        for (Item i: items) {
            if (i.getId().equals(id)) {
                return i.hasItems() ? i.getItems() : Collections.singletonList(i);
            } 
            
            if (i.hasItems()) {
                findItems(id, i.getItems());
            }
        }
        
        return Collections.emptyList();
    }
}