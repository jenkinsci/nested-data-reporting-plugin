package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.*;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.reporter.charts.ItemSeriesBuilder;
import io.jenkins.plugins.reporter.charts.TrendChart;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ItemTableModel;
import io.jenkins.plugins.reporter.model.Report;
import jline.internal.Nullable;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Build view that shows the details for a item and the subitems of item.
 *
 * @author Simon Symhoven
 */
public class ItemViewModel implements ModelObject {
    
    private static final JacksonFacade JACKSON_FACADE = new JacksonFacade();
    
    
    private final Run<?, ?> owner;
    private final Item item;
    private final String url;
    private final String label;
    private final ItemViewModel parentViewModel;
    
    private final Report report;


    /**
     * Creates a new instance of {@link ItemViewModel}.
     * 
     * @param owner
     *          the associated build/run of this view
     * @param url
     *          the relative URL of this view
     * @param item
     *          the corresponding item of this view.
     * @param label
     *          the label to be shown for this view.
     * @param parentViewModel 
     *          the view model of parent item.
     * @param report 
     *          the report attached to this run.
     */
    public ItemViewModel(final Run<?, ?> owner, final String url, final Item item, final String label, 
                         @Nullable final ItemViewModel parentViewModel, final Report report) {
        super();

        this.owner = owner;
        this.url = url;
        this.item = item;
        this.label = label;
        this.parentViewModel = parentViewModel;
        this.report = report;
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
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
        return new JacksonFacade().toJson(item.getPieChartModel(report));
    }

    /**
     * Returns the UI model for an ECharts line chart that shows the item result.
     *
     * @param configuration 
     *          determines whether the Jenkins build number should be used on the X-axis or the date
     *          
     * @return the UI model as JSON
     */
    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    public String getBuildTrend(final String configuration) {
        Job<?, ?> job = getOwner().getParent();
        RunList<?> runs = job.getBuilds();

        List<ReportAction> reports = runs.stream()
                .filter(run -> run.getNumber() <= getOwner().getNumber())
                .map(run -> Optional.of(run.getActions(ReportAction.class)))
                .map(Optional::get)
                .flatMap(List::stream)
                .filter(reportAction -> Objects.equals(reportAction.getReport().getResult().getId(), this.report.getResult().getId()))
                .collect(Collectors.toList());
        
        List<BuildResult<ReportAction>> history = reports.stream()
                .map(reportAction -> new BuildResult<>(new Build(reportAction.getOwner().getNumber(),
                        reportAction.getOwner().getDisplayName(), 0), reportAction))
                .collect(Collectors.toList());

        return new JacksonFacade().toJson(new TrendChart().create(history, 
                ChartModelConfiguration.fromJson(configuration), new ItemSeriesBuilder(item), report, item.getItems()));
    }
    
    @SuppressWarnings("unused") // Called by jelly view
    public ItemTableModel getTableModel(String id) {
        return new ItemTableModel(report, item);
    }

    /**
     * Returns the corresponding item of this view.
     * 
     * @return this item of view.
     */
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

            String url = getUrl() + "/" + link;
            return new ItemViewModel(owner, url, subItem, Messages.Module_Description(subItem.getName()), 
                    this, report);
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

    /**
     * Get the view model of parent item.
     * 
     * @return the view model of parent item.
     */
    public ItemViewModel getPreviousPage() {
        return parentViewModel;
    }

    /**
     * Get the label of the view model.
     * 
     * @return the label.
     */
    public String getLabel() {
        return label;
    }
}
