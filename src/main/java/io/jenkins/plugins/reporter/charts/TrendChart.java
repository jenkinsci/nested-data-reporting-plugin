package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.*;
import io.jenkins.plugins.reporter.ReportAction;

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
     *          the {@link SeriesBuilder} to use for the model. {@link AssetSeriesBuilder} for each asset on
     *          build level or {@link ReportSeriesBuilder} for the aggregated result on job level.
     *
     * @return the chart model, ready to be serialized to JSON
     */
    public LinesChartModel create(final Iterable<? extends BuildResult<ReportAction>> results,
                                  final ChartModelConfiguration configuration, SeriesBuilder<ReportAction> builder) {

        LinesDataSet dataSet = builder.createDataSet(configuration, results);
        LinesChartModel model = new LinesChartModel(dataSet);

        if (!dataSet.isEmpty()) {
            model.useContinuousRangeAxis();

            model.setRangeMin(0);

            LineSeries accurateSeries = new LineSeries("Accurate", Palette.GREEN.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            accurateSeries.addAll(dataSet.getSeries(AssetSeriesBuilder.ACCURATE));
            model.addSeries(accurateSeries);

            LineSeries manuallySeries = new LineSeries("Manually", Palette.YELLOW.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            manuallySeries.addAll(dataSet.getSeries(AssetSeriesBuilder.MANUALLY));
            model.addSeries(manuallySeries);

            LineSeries incorrectSeries = new LineSeries("Incorrect", Palette.RED.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            incorrectSeries.addAll(dataSet.getSeries(AssetSeriesBuilder.INCORRECT));
            model.addSeries(incorrectSeries);
        }
        
        return model;
    }
}
