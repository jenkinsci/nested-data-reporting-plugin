package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.*;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Builds the Java side model for a trend chart showing the accurate, manually and incorrect parts of an asset or report.
 * The number of builds to consider is controlled by a {@link ChartModelConfiguration} instance. The created model object
 * can be serialized to JSON (e.g., using the {@link JacksonFacade}) and can be used 1:1 as ECharts configuration 
 * object in the corresponding JS file.
 *
 * @author Simon Symhoven
 * @see JacksonFacade
 */
public class TrendChart {

    /**
     * Creates the chart for the specified results.
     *
     * @param results
     *         the forensics results to render - these results must be provided in descending order, i.e. the current *
     *         build is the head of the list, then the previous builds, and so on
     * @param configuration
     *         the chart configuration to be used
     * @param builder
     *          the {@link SeriesBuilder} to use for the model. {@link ItemSeriesBuilder} for each asset on
     *          build level or {@link ReportSeriesBuilder} for the aggregated result on job level.
     * @param report
     *          the report
     *
     * @return the chart model, ready to be serialized to JSON
     */
    public LinesChartModel create(final Iterable<? extends BuildResult<ReportAction>> results,
                                  final ChartModelConfiguration configuration, SeriesBuilder<ReportAction> builder,
                                  Report report, List<Item> items) {

        LinesDataSet dataSet = builder.createDataSet(configuration, results);
        LinesChartModel model = new LinesChartModel(dataSet);

        if (!dataSet.isEmpty()) {
            model.useContinuousRangeAxis();
            model.setRangeMin(0);
            
            /*if (dataSet.getDataSetIds().size() == 1) {
                items.forEach(item -> {
                    LineSeries series = new LineSeries(item.getName(), report.getColor(item.getId()),
                            LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
                    
                    List<Integer> values = new ArrayList<>();
                    
                    for (BuildResult<ReportAction> result : results) {
                        Result r = result.getResult().getReport().getResult();
                        
                        values.add(r.aggregate(i -> i.getId().equals(item.getId())).values().iterator().next());
                    }
                    
                    series.addAll(values);
                    model.addSeries(series);
                });
            } else {*/
                dataSet.getDataSetIds().forEach(id -> {
                    LineSeries series = new LineSeries(id, report.getColor(id),
                            LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
                    series.addAll(dataSet.getSeries(id));
                    model.addSeries(series);
                });
            // }
        }
        
        return model;
    }
}
