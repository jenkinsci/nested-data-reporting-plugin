package io.jenkins.plugins.reporter;

import hudson.model.Action;
import hudson.model.Run;
import io.jenkins.plugins.reporter.model.Item;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep.LastBuildAction;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;
import org.kohsuke.stapler.StaplerProxy;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class ReportAction implements LastBuildAction, RunAction2, StaplerProxy, Serializable {

    private static final long serialVersionUID = 7179008520286494522L;

    public final static String REPORT_ID = "report";
    
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
    public String getIconFileName() {
        return JobAction.ICON;
    }

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public String getUrlName() {
        try {
            return "report-" + URLEncoder.encode(getName(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return StringUtils.EMPTY;
        }
    }

    @Override
    public ReportDetails getTarget() {
        Item item = new Item();
        item.setId(REPORT_ID);
        item.setName(name);
        item.setItems(result.getReport().getItems());
        return new ReportDetails(getOwner(), getUrlName(), result, name, item, Optional.empty());
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

    @Override
    public Collection<? extends Action> getProjectActions() {
        return Collections.singletonList(new JobAction(getOwner().getParent(), name, result.getReport()));
    }
}
