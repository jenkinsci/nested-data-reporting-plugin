package io.jenkins.plugins.reporter.steps;

import hudson.model.Action;
import hudson.model.Run;
import io.jenkins.plugins.reporter.ReportJobAction;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.StaplerProxy;

import java.io.Serializable;
import java.util.Collection;

public class ReportAction implements SimpleBuildStep.LastBuildAction, RunAction2, StaplerProxy, Serializable {

    private static final long serialVersionUID = 7179008520286494522L;

    private transient Run<?, ?> owner;

    private final ReportResult result;
    
    public ReportAction(final Run<?, ?> owner, final ReportResult result) {
        this.owner = owner;
        this.result = result;
    }
    
    @Override
    public void onAttached(Run<?, ?> r) {
        owner = r;
        result.setOwner(r);
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        onAttached(r);
    }
    
    /**
     * Called after de-serialization to retain backward compatibility.
     *
     * @return this
     */
    protected Object readResolve() {
        return this;
    }

    /**
     * Returns the associated build/run that created the static analysis result.
     *
     * @return the run
     */
    public Run<?, ?> getOwner() {
        return owner;
    }

    
    @Override
    public Collection<? extends Action> getProjectActions() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return ReportJobAction.ICON;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public Object getTarget() {
        return null;
    }

    @Whitelisted
    public ReportResult getResult() {
        return result;
    }
}
