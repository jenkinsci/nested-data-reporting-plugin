/* global jQuery3, view, echartsJenkinsApi, bootstrap5 */
(function ($) {
    const trendConfigurationDialogId = 'chart-configuration-issues-history';

    $('#' + trendConfigurationDialogId).on('hidden.bs.modal', function () {
        redrawTrendCharts();
    });

    redrawTrendCharts();

    /**
     * Activate tooltips.
     */
    $(function () {
        $('[data-bs-toggle="tooltip"]').each(function () {
            const tooltip = new bootstrap5.Tooltip($(this)[0]);
            tooltip.enable();
        });
    });

    /**
     * Redraws the trend charts. Reads the last selected X-Axis type from the browser local storage and
     * redraws the trend charts.
     */
    function redrawTrendCharts () {

        const configuration = JSON.stringify(echartsJenkinsApi.readFromLocalStorage('jenkins-echarts-trend-configuration-default'));
        
        /**
         * Creates a build trend chart that shows the number of issues for a couple of builds.
         * Requires that a DOM <div> element exists with the ID '#severities-trend-chart'.
         */
        view.getItemIds(function(itemIds) {

            Object.entries(itemIds.responseJSON).forEach((entry) => {
                const [index, id] = entry;
                view.getBuildTrend(configuration, id, function (lineModel) {
                    echartsJenkinsApi.renderConfigurableZoomableTrendChart(`${id}-trend-chart`,
                        lineModel.responseJSON, trendConfigurationDialogId, null);
                });
            });
            
        });
    }
    
})(jQuery3);