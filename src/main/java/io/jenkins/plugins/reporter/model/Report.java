package io.jenkins.plugins.reporter.model;

import java.io.Serializable;

/**
 * Json Model class, which represents an {@link Report}. 
 * Simple data class that manages a {@link Result} add by the 
 * {@link io.jenkins.plugins.reporter.PublishReportStep}.
 *
 * @author Simon Symhoven
 */
public class Report implements Serializable {

    private static final long serialVersionUID = -4523053939010906220L;
    private final Result result;

    /**
     * Creates a new {@link Report}.
     * 
     * @param result
     *              the {@link Result} of the {@link Report}.
     */
    public Report(Result result) {
        this.result = result;
    }

    /**
     * Creates a new {@link Report}.
     */
    public Report() {
        this.result = new Result();
    }
    
    public Result getResult() {
        return result;
    }
}
