/* global jQuery3, view, echartsJenkinsApi, bootstrap5 */
(function ($) {
    const trendConfigurationDialogId = 'chart-configuration-issues-history';

    $('#' + trendConfigurationDialogId).on('hidden.bs.modal', function () {
        redrawTrendCharts();
    });

    redrawTrendCharts();

    /**
     * Create a data table instance for all tables that are marked with class "item-table".
     */
    const tables = $('table.item-table');
    tables.each(function () {
        const table = jQuery3(this);
        table.DataTable({
            pagingType: 'numbers', // Page number button only
            columnDefs: [{
                targets: 'no-sort', // Columns with class 'no-sort' are not orderable
                orderable: false
            }]
        });
    });
    
    /**
     * Activate the tab that has been visited the last time. If there is no such tab, highlight the first one.
     * If the user selects the tab using an #anchor prefer this tab.
     */
    selectTab('li:first-child a');
    const url = document.location.toString();
    if (url.match('#')) {
        const tabName = url.split('#')[1];
        selectTab('a[href="#' + tabName + '"]');
    }
    else {
        const activeTab = localStorage.getItem('activeTab');
        if (activeTab) {
            selectTab('a[href="' + activeTab + '"]');
        }
    }

    /**
     * Store the selected tab in browser's local storage.
     */
    const tabToggleLink = $('a[data-bs-toggle="tab"]');
    tabToggleLink.on('show.bs.tab', function (e) {
        window.location.hash = e.target.hash;
        const activeTab = $(e.target).attr('href');
        localStorage.setItem('activeTab', activeTab);
    });

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
     * Activates the specified tab.
     *
     * @param {String} selector - selector of the tab
     */
    function selectTab (selector) {
        const detailsTabs = $('#tab-details');
        const selectedTab = detailsTabs.find(selector);

        if (selectedTab.length !== 0) {
            const tab = new bootstrap5.Tab(selectedTab[0]);
            tab.show();
        }
    }


    /**
     * Trigger redraw charts with resize event after bs tab has changed.
     * FIXME
     */
    $(function () {
        $('a[data-bs-toggle="tab"]').on('shown.bs.tab', function () {
            $(window).trigger('resize');
        });
    });
    

    /**
     * Redraws the trend charts. Reads the last selected X-Axis type from the browser local storage and
     * redraws the trend charts.
     */
    function redrawTrendCharts () {

        const configuration = JSON.stringify(echartsJenkinsApi.readFromLocalStorage('jenkins-echarts-trend-configuration-default'));

        const openBuild = function (build) {
            view.getUrlForBuild(build, window.location.href, function (buildUrl) {
                if (buildUrl.responseJSON.startsWith('http')) {
                    window.location.assign(buildUrl.responseJSON);
                }
            });
        };
        
        /**
         * Creates a build trend chart that shows the result for a couple of builds.
         */
        view.getBuildTrend(configuration, function (lineModel) {
            echartsJenkinsApi.renderConfigurableZoomableTrendChart(`item-trend-chart`,
                lineModel.responseJSON, trendConfigurationDialogId, openBuild);
        });
    }
    
})(jQuery3);