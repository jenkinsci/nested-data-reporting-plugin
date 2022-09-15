package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.*;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;

import java.util.List;
import java.util.Optional;

/**
 * Builds the Java side model for a trend chart showing the accurate, manually and incorrect parts of an asset or report.
 * The number of builds to consider is controlled by a {@link ChartModelConfiguration} instance. The created model object
 * can be serialized to JSON (e.g., using the {@link JacksonFacade}) and can be used 1:1 as ECharts configuration 
 * object in the corresponding JS file.
 *
 * @author Simon Symhoven
 * @see JacksonFacade
 */
public class ItemHistoryChart {

    /**
     * Creates the chart for the specified results.
     *
     * @param results
     *         the forensics results to render - these results must be provided in descending order, i.e. the current *
     *         build is the head of the list, then the previous builds, and so on
     * @param configuration
     *         the chart configuration to be used
     * @param builder
     *          the {@link SeriesBuilder} to use for the model. {@link io.jenkins.plugins.reporter.model.ItemSeriesBuilder} for each asset on
     *          build level or {@link io.jenkins.plugins.reporter.model.ReportSeriesBuilder} for the aggregated result on job level.
     * @param report
     *          the report
     *
     * @return the chart model, ready to be serialized to JSON
     */
    public LinesChartModel create(final Iterable<? extends BuildResult<ReportAction>> results,
                                  final ChartModelConfiguration configuration, SeriesBuilder<ReportAction> builder,
                                  Report report, @Nullable List<Item> items) {

        LinesDataSet dataSet = builder.createDataSet(configuration, results);
        LinesChartModel model = new LinesChartModel(dataSet);

        if (!dataSet.isEmpty()) {
            model.useContinuousRangeAxis();
            model.setRangeMin(0);

            dataSet.getDataSetIds().forEach(id -> {

                Optional<Item> item = items != null ?
                        items.stream().filter(i -> i.getId().equals(id)).findFirst() : Optional.empty();

                if (item.isPresent()) {
                    // Line Series for items with only one result
                    Item i = item.get();
                    LineSeries series = new LineSeries(i.getName(), report.getColor(i.getId()),
                            LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
                    series.addAll(dataSet.getSeries(i.getId()));
                    model.addSeries(series);
                } else {
                    // Line Series for items with multiple results
                    LineSeries series = new LineSeries(id, report.getColor(id),
                            LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
                    series.addAll(dataSet.getSeries(id));
                    model.addSeries(series);
                }

            });

        }

        return model;
    }
}
