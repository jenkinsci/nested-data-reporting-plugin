package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;

import java.util.HashMap;
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
        if (item.getResult().size() == 1) {
            return item.getItems().stream().collect(Collectors.toMap(Item::getName, Item::getTotal));
        }
        
        if (item.getId().equals(ReportAction.REPORT_ID)) {
            return reportAction.getReport().getResult().aggregate();
        }
        
        if (!item.hasItems()) {
            return item.getResult();
        }
        
        return reportAction.getReport().getResult().aggregate(i -> i.getId().equals(item.getId()));
    }
}