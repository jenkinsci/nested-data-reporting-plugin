package io.jenkins.plugins.reporter.nextgen;

import hudson.model.Action;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.StaplerProxy;
import jenkins.tasks.SimpleBuildStep.LastBuildAction;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

public class ReportAction implements LastBuildAction, RunAction2, StaplerProxy, Serializable {

    private static final long serialVersionUID = 7179008520286494522L;

    private transient Run<?, ?> owner;
    
    private final String name;
    
    private final ReportResult result;
    
    public ReportAction(final Run<?, ?> owner, final ReportResult result, String name) {
        this.owner = owner;
        this.result = result;
        this.name = name;
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
        return Collections.singleton(new JobAction(owner.getParent(), name));
    }

    @Override
    public String getIconFileName() {
        return JobAction.ICON;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getUrlName() {
        return "report-" + getName().hashCode();
    }

    @Override
    public Object getTarget() {
        return new ReportDetails(getOwner(), getUrlName(),result, name);
    }

    @Whitelisted
    public ReportResult getResult() {
        return result;
    }

    /**
     * Returns the name of the report.
     *
     * @return the ID
     */
    public String getName() {
        return name;
    }
}
