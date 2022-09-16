package io.jenkins.plugins.reporter.model;

import edu.hm.hafner.echarts.BuildResult;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;

import java.util.Iterator;
import java.util.Optional;


/**
 * History of analysis results.
 */
public interface History extends Iterable<BuildResult<ReportResult>> {
    /**
     * Returns the baseline action (if already available).
     *
     * @return the baseline action
     */
    Optional<ReportAction> getBaselineAction();

    /**
     * Returns the baseline result (if already available).
     *
     * @return the baseline result
     */
    Optional<ReportResult> getBaselineResult();

    /**
     * Returns the historical result (if there is any).
     *
     * @return the historical result
     */
    Optional<ReportResult> getResult();

    /**
     * Returns the build that contains the historical result (if there is any).
     *
     * @return the historical result
     */
    Optional<Run<?, ?>> getBuild();

    /**
     * Returns the report of the historical result. If there is no historical build found, then an empty report is returned.
     *
     * @return the report of the historical build
     */
    Report getReport();

    @Override
    @NonNull
    Iterator<BuildResult<ReportResult>> iterator();

    /**
     * Returns whether this history has more than one result.
     *
     * @return {@code true} if there are multiple results, {@code false} otherwise
     */
    boolean hasMultipleResults();
}

