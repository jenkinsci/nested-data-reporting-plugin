package io.jenkins.plugins.reporter.steps;

import hudson.model.Action;
import hudson.model.Run;
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
        
    }

    @Override
    public void onLoad(Run<?, ?> r) {

    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
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
