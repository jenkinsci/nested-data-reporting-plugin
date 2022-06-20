package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.reporter.charts.ItemSeriesBuilder;
import io.jenkins.plugins.reporter.charts.ReportSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Item;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ReportViewModel extends DefaultAsyncTableContentProvider implements ModelObject {
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();

    private final Run<?, ?> owner;
    private final Item item;
    private final String url;
    private final String label;
    private final Map<String, String> colors;


    /**
     * Creates a new instance of {@link ReportViewModel}.
     * 
     * @param owner
     * @param url
     * @param item
     * @param label
     * @param colors
     */
    ReportViewModel(final Run<?, ?> owner, final String url, final Item item, final String label, Map<String, String> colors) {
        super();

        this.owner = owner;
        this.url = url;
        this.item = item;
        this.label = label;
        this.colors = colors;
    }

    public Run<?, ?> getOwner() {
        return owner;
    }

    @Override
    public String getDisplayName() {
        return label;
    }

    /**
     * Returns the UI model for an ECharts item data chart.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getItemDataModel() {
        PieChartModel model = new PieChartModel(item.getId());
        item.getResult().forEach((key, value) -> model.add(new PieData(key, value),
                colors.get(key)));
        return new JacksonFacade().toJson(model);
    }
    

    private String createTrendAsJson(final TrendChart trendChart, final String configuration, String id) {
        Job<?, ?> job = getOwner().getParent();
        RunList<?> runs = job.getBuilds();

        List<Optional<ReportAction>> reports = runs.stream()
                .filter(run -> run.getNumber() <= getOwner().getNumber())
                .map(run -> Optional.ofNullable(run.getAction(ReportAction.class)))
                .collect(Collectors.toList());

        List<BuildResult<ReportAction>> history = new ArrayList<>();
        for (Optional<ReportAction> report : reports) {
            if (report.isPresent()) {
                ReportAction reportAction = report.get();
                Build build = new Build(reportAction.getOwner().getNumber(), reportAction.getOwner().getDisplayName(), 0);
                history.add(new BuildResult<>(build, reportAction));
            }
        }

        SeriesBuilder<ReportAction> builder = new ItemSeriesBuilder(item);

        return new JacksonFacade().toJson(trendChart.create(history, ChartModelConfiguration.fromJson(configuration),
                builder, colors));
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the item result.
     *
     * @param configuration determines whether the Jenkins build number should be used on the X-axis or the date
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public String getBuildTrend(final String configuration) {
        return createTrendAsJson(new TrendChart(), configuration, item.getId());
    }
    
    
    @Override
    @SuppressWarnings("unused") // Called by jelly view
    public TableModel getTableModel(String id) {
        return new ReportTableModel(id, item, colors);
    }
    
    public Item getItem() {
        return item;
    }
    
    /**
     * Returns the (relative) URL of this model object.
     *
     * @return this model objects' URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns a new sub page for the selected link.
     *
     * @param link
     *         the link to identify the sub page to show
     * @param request
     *         Stapler request
     * @param response
     *         Stapler response
     *
     * @return the new sub page
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Object getDynamic(final String link, final StaplerRequest request, final StaplerResponse response) {
        try {
            Item subItem = item.getItems()
                    .stream()
                    .filter(i -> i.getId().hashCode() == Integer.parseInt(link))
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
            
            return new ItemFactory().createNewItemView(link, owner, this, subItem, colors);
        }
        catch (NoSuchElementException ignored) {
            try {
                response.sendRedirect2("../");
            }
            catch (IOException ignore) {
                // ignore
            }
            return this; // fallback on broken URLs
        }
    }
}
