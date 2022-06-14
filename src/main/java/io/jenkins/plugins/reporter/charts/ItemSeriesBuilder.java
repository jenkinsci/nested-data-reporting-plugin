package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.SeriesBuilder;
import io.jenkins.plugins.reporter.ReportAction;

import java.util.Map;

/**
 * Builds one x-axis point for the series of a line chart 
 * showing the results of an item from the json model.
 *
 * @author Simon Symhoven
 */
public class ItemSeriesBuilder extends SeriesBuilder<ReportAction> {
    
    private final String id;

    /**
     * Creates a new {@link ItemSeriesBuilder}.
     * 
     * @param id
     *         the id as string of the item from the json model to get the series for.
     */
    public ItemSeriesBuilder(String id) {
        this.id = id;
    }

    @Override
    protected Map<String, Integer> computeSeries(ReportAction reportAction) {
        return reportAction.getReport().aggregate(item -> item.getId().equals(id));
    }
}