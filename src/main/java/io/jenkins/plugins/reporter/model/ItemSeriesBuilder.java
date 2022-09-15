package io.jenkins.plugins.reporter.model;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
                return reportAction.getResult().getReport().getItems().stream()
                        .collect(Collectors.toMap(Item::getId, Item::getTotal));
            }

            return reportAction.getResult().getReport().aggregate();
        }
        
        Item parent = reportAction.getResult().getReport().findItem(item.getId()).orElse(new Item());
        List<Item> items = parent.hasItems() ? parent.getItems() : Collections.singletonList(parent);
        
        if (item.getResult().size() == 1) {
            return items.stream().collect(Collectors.toMap(Item::getId, Item::getTotal));
        }

        return reportAction.getResult().getReport().aggregate(items);
    }

    private List<Item> findItems(String id, List<Item> items)
    {
        if (items != null) {
            for (Item i: items) {
                if (i.getId().equals(id)) {
                    return i.hasItems() ? i.getItems() : Collections.singletonList(i);
                } else {
                    List<Item> sub = findItems(id, i.getItems());
                    if (sub.size() > 0) {
                        return sub;
                    }
                }
            }
        }

        return Collections.emptyList();
    }
}