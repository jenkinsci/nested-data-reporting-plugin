package io.jenkins.plugins.reporter.model;

import java.util.Optional;

import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportAction;

/**
 * Selects a {@link ReportAction} from all registered actions in a given job.
 *
 * @author Simon Symhoven
 */
public interface ResultSelector {
    /**
     * Tries to find a report action of the specified build that should be used to compute the history.
     *
     * @param build
     *         the build
     *
     * @return the report action, if there is one attached to the job
     */
    Optional<ReportAction> get(Run<?, ?> build);
}
