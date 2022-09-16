package io.jenkins.plugins.reporter.model;

import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportAction;

import java.util.List;
import java.util.Optional;

public class ByIdResultSelector implements ResultSelector {

    private final String id;

    /**
     * Creates a new instance of {@link ByIdResultSelector}.
     *
     * @param id
     *         the ID of the result
     */
    public ByIdResultSelector(final String id) {
        this.id = id;
    }
    
    @Override
    public Optional<ReportAction> get(Run<?, ?> build) {
        List<ReportAction> actions = build.getActions(ReportAction.class);
        for (ReportAction action : actions) {
            if (id.equals(action.getId())) {
                return Optional.of(action);
            }
        }
        return Optional.empty();
    }
}
