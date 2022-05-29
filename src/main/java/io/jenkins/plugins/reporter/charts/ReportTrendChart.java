package io.jenkins.plugins.reporter.charts;

import edu.hm.hafner.echarts.*;
import io.jenkins.plugins.reporter.ReportBuildAction;

public class ReportTrendChart {
    
    public LinesChartModel create(final Iterable<? extends BuildResult<ReportBuildAction>> results,
                                  final ChartModelConfiguration configuration) {
        
        ReportSeriesBuilder builder = new ReportSeriesBuilder();
        LinesDataSet dataSet = builder.createDataSet(configuration, results);
        LinesChartModel model = new LinesChartModel(dataSet);
        
        if (!dataSet.isEmpty()) {
            model.useContinuousRangeAxis();
            
            model.setRangeMax(Math.max(
                    Math.max(createRangeMaxFor(dataSet, ReportSeriesBuilder.ACCURATE),
                            createRangeMaxFor(dataSet, ReportSeriesBuilder.MANUALLY)),
                    createRangeMaxFor(dataSet, ReportSeriesBuilder.INCORRECT)) + 5);
            
            model.setRangeMin(Math.max(0, Math.min(
                    Math.min(createRangeMinFor(dataSet, ReportSeriesBuilder.ACCURATE),
                            createRangeMinFor(dataSet, ReportSeriesBuilder.MANUALLY)),
                    createRangeMinFor(dataSet, ReportSeriesBuilder.INCORRECT)) - 5));

            LineSeries accurateSeries = new LineSeries("Accurate", Palette.GREEN.getNormal(),
                    LineSeries.StackedMode.SEPARATE_LINES, LineSeries.FilledMode.LINES);
            accurateSeries.addAll(dataSet.getSeries(ReportSeriesBuilder.ACCURATE));
            model.addSeries(accurateSeries);

            LineSeries manuallySeries = new LineSeries("Manually", Palette.YELLOW.getNormal(),
                    LineSeries.StackedMode.SEPARATE_LINES, LineSeries.FilledMode.LINES);
            manuallySeries.addAll(dataSet.getSeries(ReportSeriesBuilder.MANUALLY));
            model.addSeries(manuallySeries);

            LineSeries incorrectSeries = new LineSeries("Incorrect", Palette.RED.getNormal(),
                    LineSeries.StackedMode.SEPARATE_LINES, LineSeries.FilledMode.LINES);
            incorrectSeries.addAll(dataSet.getSeries(ReportSeriesBuilder.INCORRECT));
            model.addSeries(incorrectSeries);
        }
        return model;
    }

    private int createRangeMinFor(final LinesDataSet dataSet, final String label) {
        return min(dataSet, label) - 10;
    }

    private Integer min(final LinesDataSet dataSet, final String dataSetId) {
        return dataSet.getSeries(dataSetId).stream().reduce(Math::min).orElse(0);
    }

    private int createRangeMaxFor(final LinesDataSet dataSet, final String label) {
        return max(dataSet, label) + 10;
    }

    private Integer max(final LinesDataSet dataSet, final String dataSetId) {
        return dataSet.getSeries(dataSetId).stream().reduce(Math::max).orElse(500);
    }
    
}
