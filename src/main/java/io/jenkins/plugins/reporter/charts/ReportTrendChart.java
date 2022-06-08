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
            model.setRangeMin(0);

            LineSeries accurateSeries = new LineSeries("Accurate", Palette.GREEN.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            accurateSeries.addAll(dataSet.getSeries(ReportSeriesBuilder.ACCURATE));
            model.addSeries(accurateSeries);

            LineSeries manuallySeries = new LineSeries("Manually", Palette.YELLOW.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            manuallySeries.addAll(dataSet.getSeries(ReportSeriesBuilder.MANUALLY));
            model.addSeries(manuallySeries);

            LineSeries incorrectSeries = new LineSeries("Incorrect", Palette.RED.getNormal(),
                    LineSeries.StackedMode.STACKED, LineSeries.FilledMode.FILLED);
            incorrectSeries.addAll(dataSet.getSeries(ReportSeriesBuilder.INCORRECT));
            model.addSeries(incorrectSeries);
        }
        return model;
    }
    
}
