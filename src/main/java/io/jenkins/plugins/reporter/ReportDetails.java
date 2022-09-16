package io.jenkins.plugins.reporter;

import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.hm.hafner.echarts.ChartModelConfiguration;
import edu.hm.hafner.echarts.JacksonFacade;
import hudson.model.Job;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.util.RunList;
import io.jenkins.plugins.reporter.charts.ItemHistoryChart;
import io.jenkins.plugins.reporter.charts.ItemPieChart;
import io.jenkins.plugins.reporter.model.ByIdResultSelector;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ItemSeriesBuilder;
import io.jenkins.plugins.reporter.model.ItemTableModel;
import io.jenkins.plugins.reporter.util.BuildResultNavigator;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReportDetails implements ModelObject {

    private final transient Run<?, ?> owner;

    private final String displayName;

    private final ReportResult result;
    
    private final Item item;
    
    private final Optional<ReportDetails> parentViewModel;
    
    private final String url;

    private final List<String> errorMessages = new ArrayList<>();
    
    private final List<String> infoMessages = new ArrayList<>();
    
    /**
     * Creates a new instance of {@link ReportDetails}.
     *
     * @param owner
     *          the associated build/run of this view
     * @param url
     *          the relative URL of this view
     * @param result
     *          the report result
     * @param displayName
     *          the human-readable name of this view (shown in breadcrumb).
     * @param item 
     *          the item to show for this view.
     * @param parentViewModel
     *          the view model of parent item.
     */
    public ReportDetails(final Run<?, ?> owner, final String url, final ReportResult result, final String displayName, 
                         final Item item, final Optional<ReportDetails> parentViewModel) {
        super();
        
        this.parentViewModel = parentViewModel;
        this.owner = owner;
        this.url = url;
        this.result = result;
        this.item = item;
        this.displayName = displayName;

        infoMessages.addAll(result.getInfoMessages().castToList());
        errorMessages.addAll(result.getErrorMessages().castToList());
    }

    ReportResult getResult() {
        return result;
    }
    
    /**
     * Returns the error messages of the static analysis run.
     *
     * @return the error messages
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Collection<String> getErrorMessages() {
        return errorMessages;
    }

    /**
     * Returns the information messages of the static analysis run.
     *
     * @return the information messages
     */
    @SuppressWarnings("unused") // Called by jelly view
    public Collection<String> getInfoMessages() {
        return infoMessages;
    }

    public Item getItem() {
        return item;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the build as owner of this object.
     *
     * @return the owner
     */
    public final Run<?, ?> getOwner() {
        return owner;
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
     * Get the view model of parent item.
     *
     * @return the view model of parent item.
     */
    public Optional<ReportDetails> getPrevious() {
        return parentViewModel;
    }
    
    /**
     * Returns the UI model for an ECharts item data chart.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public String getItemPieChartModel() {
        return new JacksonFacade().toJson(new ItemPieChart().create(result.getReport(), getItem()));
    }

    /**
     * Returns the UI model for the specified table.
     *
     * @return the UI model as JSON
     */
    @SuppressWarnings("unused") // Called by jelly view
    public ItemTableModel getTableModel() {
        return new ItemTableModel(result.getReport(), getItem());
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
                .filter(reportAction -> Objects.equals(reportAction.getResult().getReport().getId(), result.getReport().getId()))
                .collect(Collectors.toList());

        List<BuildResult<ReportAction>> history = reports.stream()
                .map(reportAction -> new BuildResult<>(new Build(reportAction.getOwner().getNumber(),
                        reportAction.getOwner().getDisplayName(), 0), reportAction))
                .collect(Collectors.toList());

        return new JacksonFacade().toJson(new ItemHistoryChart().create(history,
                ChartModelConfiguration.fromJson(configuration), new ItemSeriesBuilder(item), result.getReport(), item.getItems()));
    }

    /**
     * Returns the URL for same results of the selected build.
     *
     * @param build
     *         the selected build to open the new results for
     * @param detailsUrl
     *         the absolute URL to this details view report
     *
     * @return the URL to the report or an empty string if the report are not available
     */
    @JavaScriptMethod
    public String getUrlForBuild(final String build, final String detailsUrl) {
        ReportHistory history = createHistory();
        for (BuildResult<ReportResult> buildResult : history) {
            if (buildResult.getBuild().getDisplayName().equals(build)) {
                return new BuildResultNavigator().getSameUrlForOtherBuild(owner, detailsUrl, getUrl(),
                        buildResult.getBuild().getNumber()).orElse(StringUtils.EMPTY);
            }
        }
        return StringUtils.EMPTY;
    }

    private ReportHistory createHistory() {
        return new ReportHistory(owner, new ByIdResultSelector(result.getReport().getId()));
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
                    .filter(i -> Objects.equals(i.getId(), link))
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);

            String url = getUrl() + "/" + link;
            return new ReportDetails(owner, url, result, Messages.Module_Description(subItem.getName()), subItem,
                    Optional.of(this));
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
