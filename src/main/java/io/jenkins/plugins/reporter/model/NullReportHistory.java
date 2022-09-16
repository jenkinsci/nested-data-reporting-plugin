package io.jenkins.plugins.reporter.model;


import edu.hm.hafner.echarts.BuildResult;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;

import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;

public class NullReportHistory implements History {
    @Override
    public Optional<ReportAction> getBaselineAction() {
        return Optional.empty();
    }

    @Override
    public Optional<ReportResult> getBaselineResult() {
        return Optional.empty();
    }

    @Override
    public Optional<ReportResult> getResult() {
        return Optional.empty();
    }

    @Override
    public Optional<Run<?, ?>> getBuild() {
        return Optional.empty();
    }

    @Override
    public Report getReport() { 
        return new Report();
    }

    @NonNull
    @Override
    public Iterator<BuildResult<ReportResult>> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean hasMultipleResults() {
      return false;
    }
}
