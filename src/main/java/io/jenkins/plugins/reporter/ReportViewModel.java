package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.Functions;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableConfiguration;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.datatables.options.SelectStyle;
import io.jenkins.plugins.reporter.charts.ItemSeriesBuilder;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Report;
import org.apache.commons.collections.ListUtils;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.util.*;
import java.util.stream.Collectors;

public class ReportViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    
    private final Run<?, ?> owner;
    private final Report report;

    /**
     * Creates a new instance of {@link ReportViewModel}.
     *
     * @param owner
     *         the build as owner of this view
     * @param report
     *         the report to show in the view
     */
    ReportViewModel(final Run<?, ?> owner, final Report report) {
        super();

        this.owner = owner;
        this.report = report;
    }
    
    public Run<?, ?> getOwner() {
        return owner;
    }
    
    @Override
    public String getDisplayName() {
        return report.getLabel();
    }

    public Report getReport() {
        return report;
    }
    
    /**
     * Returns the UI model for an ECharts item data chart.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getItemDataModel(Item item) {
        PieChartModel model = new PieChartModel(item.getId());
        item.getResult().forEach((key, value) -> model.add(new PieData(key, value), 
                report.getResult().getColors().get(key)));
        return new JacksonFacade().toJson(model);
    }

    /**
     * Returns the UI model for an ECharts report data chart.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getReportDataModel() {
        PieChartModel model = new PieChartModel("aggregated-pie-chart");
        getReport().aggregate().forEach((key, value) -> model.add(new PieData(key, value),
                report.getResult().getColors().get(key)));
        return new JacksonFacade().toJson(model);
    }

    private String createTrendAsJson(final TrendChart trendChart, final String configuration, String id) {
        Job<?, ?> job = getOwner().getParent();
        RunList<?> runs = job.getBuilds();

        List<Optional<ReportAction>> reports = runs.stream()
                .filter(run -> Objects.requireNonNull(run.getResult()).isCompleteBuild())
                .filter(run -> run.getNumber() <= getOwner().getNumber())
                .map(run -> Optional.of(run.getAction(ReportAction.class)))
                .collect(Collectors.toList());

        List<BuildResult<ReportAction>> history = new ArrayList<>();
        for (Optional<ReportAction> report : reports) {
             if (report.isPresent()) {
                 ReportAction reportAction = report.get();
                 Build build = new Build(reportAction.getOwner().getNumber(), reportAction.getOwner().getDisplayName(), 0);
                 history.add(new BuildResult<>(build, reportAction));
             }
        }
        
        SeriesBuilder<ReportAction> builder = Objects.equals(id, "aggregated") ? 
                new ReportSeriesBuilder() : new ItemSeriesBuilder(id);
        
        return new JacksonFacade().toJson(trendChart.create(history, ChartModelConfiguration.fromJson(configuration), 
                builder, report.getResult().getColors()));
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the item properties.
     *
     * @param configuration
     *         determines whether the Jenkins build number should be used on the X-axis or the date
     *
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public String getBuildTrend(final String configuration, String id) {
        return createTrendAsJson(new TrendChart(), configuration, id);
    }

    /**
     * Returns the ids of the items to render the trend charts.
     *
     * @return the ids of items as list
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public List<String> getItemIds() {
        List<String> items = getReport().getResult().getComponents().stream().map(Item::getId).collect(Collectors.toList());
        items.add("aggregated");
        return items;
    }

    @Override
    public TableModel getTableModel(String id) {
        return new ReportTableModel(id);
    }

    /**
     * UI table model for the report table.
     */
    static class ReportTableModel extends TableModel {
        
        private final String id;

        ReportTableModel(final String id) {
            super();

            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public TableConfiguration getTableConfiguration() {
            TableConfiguration tableConfiguration = new TableConfiguration();
            tableConfiguration.select(SelectStyle.SINGLE);
            return tableConfiguration;
        }

        @Override
        public List<TableColumn> getColumns() {
            List<TableColumn> columns = new ArrayList<>();
            
            // this column is hidden, but used to access the file hash from the frontend
            TableColumn fileHashColumn = new TableColumn("Hash", "fileHash");
            fileHashColumn.setHeaderClass(TableColumn.ColumnCss.HIDDEN);
            // fileHashColumn.setWidth(0);
            columns.add(fileHashColumn);

            TableColumn packageColumn = new TableColumn("Package", "packageName");
            // packageColumn.setWidth(2);
            columns.add(packageColumn);

            TableColumn fileColumn = new TableColumn("File", "fileName");
            // fileColumn.setWidth(2);
            columns.add(fileColumn);

            TableColumn lineColumn = new TableColumn("Line", "lineCoverage", "number");
            // lineColumn.setWidth(2);
            columns.add(lineColumn);

            TableColumn lineColumnDelta = new TableColumn("Line Δ", "lineCoverageDelta", "number");
            // lineColumnDelta.setWidth(1);
            columns.add(lineColumnDelta);

            TableColumn branchColumn = new TableColumn("Branch", "branchCoverage", "number");
            // branchColumn.setWidth(2);
            columns.add(branchColumn);

            TableColumn branchColumnDelta = new TableColumn("Branch Δ", "branchCoverageDelta", "number");
            // branchColumnDelta.setWidth(1);
            columns.add(branchColumnDelta);

            TableColumn loc = new TableColumn("LOC", "loc", "number");
            // loc.setWidth(1);
            columns.add(loc);

            return columns;
        }

        @Override
        public List<Object> getRows() {
            List<Object> rows = new ArrayList<>();
            rows.add(new Row(id));
            return rows;
        }

        /**
         * UI row model for the coverage details table.
         */
        private static class Row {

            String id;
            
            Row(String id) {
                this.id = id;
            }

            public String getFileHash() {
                return String.valueOf(id.hashCode());
            }

            public String getFileName() {
                return id;
            }

            public String getPackageName() {
                return id;
            }

            public DetailedColumnDefinition getLineCoverage() {
                return new DetailedColumnDefinition("lineCov", "lineCov");
            }

            public DetailedColumnDefinition getBranchCoverage() {
                return new DetailedColumnDefinition("branchCov", "branchCov");
            }

            public DetailedColumnDefinition getLineCoverageDelta() {
                return new DetailedColumnDefinition("deltCov", "deltCov");
            }

            public DetailedColumnDefinition getBranchCoverageDelta() {
                return new DetailedColumnDefinition("branchDeltCov", "branchDeltCov");
            }

            public DetailedColumnDefinition getLoc() {
                return new DetailedColumnDefinition("loc", "loc");
            }

           
        }



    }
}
