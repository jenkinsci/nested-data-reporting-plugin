package io.jenkins.plugins.reporter.model;

import java.io.Serializable;

/**
 * Json Model class, which represents an {@link Report}. 
 * Simple data class that manages a {@link Result} and a label add by the 
 * {@link io.jenkins.plugins.reporter.PublishReportStep}.
 *
 * @author Simon Symhoven
 */
public class Report implements Serializable {

    private static final long serialVersionUID = -4523053939010906220L;
    private final Result result;
    private final String label;

    /**
     * Creates a new {@link Report}.
     * 
     * @param result
     *              the {@link Result} of the {@link Report}.
     * @param label
     *              the label to show for this action in jenkins ui.
     */
    public Report(Result result, String label) {
        this.result = result;
        this.label = label;
    }

    /**
     * Creates a new {@link Report}.
     */
    public Report() {
        this.label = "Data Report";
        this.result = new Result();
    }
    
    public Result getResult() {
        return result;
    }
    
    public String getLabel() {
        return label;
    }
    
}
