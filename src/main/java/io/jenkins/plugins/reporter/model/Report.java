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
    private static final String DEFAULT_COLOR = "#9E9E9E";
    private final Result result;

    private final DisplayType displayType;
    
    /**
     * Creates a new {@link Report}.
     * 
     * @param result
     *              the {@link Result} of the {@link Report}.
     * @param displayType
     *              the display type to display values.
     */
    public Report(Result result, DisplayType displayType) {
        this.result = result;
        this.displayType = displayType;
    }

    /**
     * Creates a new {@link Report}.
     */
    public Report() {
        this.result = new Result();
        this.displayType = DisplayType.ABSOLUTE;
    }
    
    public Result getResult() {
        return result;
    }
    
    public String getColor(String id) {
        return result.getColors().getOrDefault(id, DEFAULT_COLOR);
    }

    public DisplayType getDisplayType() {
        if (displayType == null) return DisplayType.ABSOLUTE;
        return displayType;
    }
}
