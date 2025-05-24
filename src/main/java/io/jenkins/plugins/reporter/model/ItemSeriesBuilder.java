package io.jenkins.plugins.reporter.model;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;

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
public class ItemSeriesBuilder extends SeriesBuilder<ReportResult> {

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
    protected Map<String, Double> computeSeries(ReportResult reportResult) {

        if (item.getId().equals(ReportAction.REPORT_ID)) {

            if (item.getResult().size() == 1) {
                return reportResult.getReport().getItems().stream()
                        .collect(Collectors.toMap(Item::getId, Item::getTotal));
            }

            return reportResult.getReport().aggregate();
        }
        
        Item parent = reportResult.getReport().findItem(item.getId()).orElse(new Item());
        List<Item> items = parent.hasItems() ? parent.getItems() : Collections.singletonList(parent);
        
        if (item.getResult().size() == 1) {
            return items.stream().collect(Collectors.toMap(Item::getId, Item::getTotal));
        }

        return reportResult.getReport().aggregate(items);
    }
}