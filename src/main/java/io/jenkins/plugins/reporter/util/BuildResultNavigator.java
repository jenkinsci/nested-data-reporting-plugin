package io.jenkins.plugins.reporter.util;

import hudson.model.Run;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Navigates from the current results to the same results of any other build of the same job.
 *
 * @author Simon Symhoven
 */
public class BuildResultNavigator {

    private static final String SLASH = "/";

    /**
     * Navigates from the current results to the same results of any other build of the same job.
     *
     * @param currentBuild
     *         the current build that owns the view results
     * @param viewUrl
     *         the absolute URL to the view results
     * @param resultId
     *         the ID of the static analysis results
     * @param selectedBuildNumber
     *         the selected build to open the new results for
     *
     * @return the URL to the results if possible
     */
    public Optional<String> getSameUrlForOtherBuild(final Run<?, ?> currentBuild, final String viewUrl,
                                                    final String resultId, final String selectedBuildNumber) {
        try {
            return getSameUrlForOtherBuild(currentBuild, viewUrl, resultId, Integer.parseInt(selectedBuildNumber));
        }
        catch (NumberFormatException exception) {
            // ignore
        }
        return Optional.empty();
    }

    /**
     * Navigates from the current results to the same results of any other build of the same job.
     *
     * @param currentBuild
     *         the current build that owns the view results
     * @param viewUrl
     *         the absolute URL to the view results
     * @param resultId
     *         the ID of the static analysis results
     * @param selectedBuildNumber
     *         the selected build to open the new results for
     *
     * @return the URL to the results if possible
     */
    public Optional<String> getSameUrlForOtherBuild(final Run<?, ?> currentBuild, final String viewUrl, final String resultId,
                                                    final int selectedBuildNumber) {
        Run<?, ?> selectedBuild = currentBuild.getParent().getBuildByNumber(selectedBuildNumber);
        if (selectedBuild != null) {
            String match = SLASH + currentBuild.getNumber() + SLASH + resultId;
            String regex = escapeMatch(match) + ".*";
            if (viewUrl.contains(match)) {
                String url = viewUrl.replaceFirst(
                        regex, SLASH + selectedBuildNumber + SLASH + resultId);
                return Optional.of(url);
            }
        }
        return Optional.empty();
    }

    public String escapeMatch(String str) {
        Pattern escaper = Pattern.compile("([^a-zA-z0-9])");
        return escaper.matcher(str).replaceAll("\\\\$1");
    }
    
}
