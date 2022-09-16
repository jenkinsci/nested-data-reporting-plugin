package io.jenkins.plugins.reporter;


import edu.hm.hafner.echarts.Build;
import edu.hm.hafner.echarts.BuildResult;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import io.jenkins.plugins.reporter.model.History;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.ResultSelector;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public class ReportHistory implements History {

    private static final int MIN_BUILDS = 2;

    private final Run<?, ?> baseline;

    private final ResultSelector selector;

    /**
     * Creates a new instance of {@link ReportHistory}.
     *
     * @param baseline
     *         the build to start the history from
     * @param selector
     *         selects the associated action from a build
     */
    public ReportHistory(Run<?, ?> baseline, ResultSelector selector) {
        this.baseline = baseline;
        this.selector = selector;
    }

    @Override
    public Optional<ReportAction> getBaselineAction() {
        return selector.get(baseline);
    }

    @Override
    public Optional<ReportResult> getBaselineResult() {
        return getBaselineAction().map(ReportAction::getResult);
    }

    @Override
    public Optional<ReportResult> getResult() {
        return getPreviousAction().map(ReportAction::getResult);
    }

    @Override
    public Optional<Run<?, ?>> getBuild() {
        return getPreviousAction().map(ReportAction::getOwner);
    }

    @Override
    public Report getReport() {
        return getResult().map(ReportResult::getReport).orElseGet(Report::new);
    }

    @NonNull
    @Override
    public Iterator<BuildResult<ReportResult>> iterator() {
        return new ReportResultIterator(baseline, selector);
    }

    @Override
    public boolean hasMultipleResults() {
        Iterator<BuildResult<ReportResult>> iterator = iterator();
        for (int count = 1; iterator.hasNext(); count++) {
            if (count >= MIN_BUILDS) {
                return true;
            }
            iterator.next();
        }
        return false;
    }

    private Optional<ReportAction> getPreviousAction() {
        Optional<Run<?, ?>> run = getRunWithResult(baseline, selector);
        if (run.isPresent()) {
            return selector.get(run.get());
        }
        return Optional.empty();
    }

    private static Optional<Run<?, ?>> getRunWithResult(final @CheckForNull Run<?, ?> start,
                                                        final ResultSelector selector) {
        for (Run<?, ?> run = start; run != null; run = run.getPreviousBuild()) {
            Optional<ReportAction> action = selector.get(run);
            if (action.isPresent()) {
               return Optional.of(run);
            }
        }
        return Optional.empty();
    }
    
    /**
     * Provides an iterator of analysis results starting from a baseline and going back in history.
     */
    private static class ReportResultIterator implements Iterator<BuildResult<ReportResult>> {
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<Run<?, ?>> cursor;
        
        private final ResultSelector selector;

        /**
         * Creates a new iterator starting from the baseline.
         *
         * @param baseline
         *         the run to start from
         * @param selector
         *         selects the associated action from a build
         */
        ReportResultIterator(final Run<?, ?> baseline, final ResultSelector selector) {
            cursor = getRunWithResult(baseline, selector);
            this.selector = selector;
        }

        @Override
        public boolean hasNext() {
            return cursor.isPresent();
        }

        @Override
        public BuildResult<ReportResult> next() {
            if (cursor.isPresent()) {
                Run<?, ?> run = cursor.get();
                Optional<ReportAction> resultAction = selector.get(run);

                cursor = getRunWithResult(run.getPreviousBuild(), selector);

                if (resultAction.isPresent()) {
                    return new BuildResult<>(new Build(run.getNumber(), run.getDisplayName(), 
                            (int) (run.getTimeInMillis() / 1000)), resultAction.get().getResult());
                }
            }

            throw new NoSuchElementException("No more runs with an report: " + cursor);
        }
    }
}
