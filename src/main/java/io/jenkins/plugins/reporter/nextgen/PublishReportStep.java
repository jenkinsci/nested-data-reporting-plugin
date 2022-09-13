package io.jenkins.plugins.reporter.nextgen;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.Messages;
import org.eclipse.collections.impl.factory.Sets;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.Set;

public class PublishReportStep extends Step implements Serializable {

    private static final long serialVersionUID = 423552861898621744L;
    
    private String name;
    
    private Provider provider;

    private String displayType;

    /**
     * Creates a new instance of {@link PublishReportStep}.
     */
    @DataBoundConstructor
    public PublishReportStep() {
        super();

        // empty constructor required for Stapler
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }
    
    @DataBoundSetter
    public void setProvider(final Provider provider) {
        this.provider = provider;
    }

    public Provider getProvider() {
        return provider;
    }

    @DataBoundSetter
    public void setDisplayType(final String displayType) {
        this.displayType = displayType;
    }
    
    public String getDisplayType() {
        return displayType;
    }
    
    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(context, this);
    }

    static class Execution extends SynchronousNonBlockingStepExecution<ReportResult> {

        private static final long serialVersionUID = -6468854519922975080L;

        private final PublishReportStep step;
        
        protected Execution(@NonNull StepContext context, final PublishReportStep step) {
            super(context);
            this.step = step;
        }

        @Override
        protected ReportResult run() throws Exception {
            ReportRecorder recorder = new ReportRecorder();
            recorder.setName(step.getName());
            recorder.setProvider(step.getProvider());
            
            return recorder.perform(getContext().get(Run.class), getContext().get(FilePath.class), 
                    getContext().get(TaskListener.class));
        }
    }
            
    /**
     * Descriptor for this step: defines the context and the UI labels.
     */
    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<Class<?>> getRequiredContext() {
            return Sets.immutable.of(FlowNode.class, Run.class, TaskListener.class).castToSet();
        }

        @Override
        public String getFunctionName() {
            return "publishReport";
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.Step_Name();
        }
    }
    
}
