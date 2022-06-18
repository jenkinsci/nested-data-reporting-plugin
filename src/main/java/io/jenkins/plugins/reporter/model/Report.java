package io.jenkins.plugins.reporter.model;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple data class that manages a list of {@link Item} and a label add by the 
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
     * @param result
     *              the {@link Result} of the {@link Report}.
     * @param label
     *              the label to show for this action in jenkins ui.
     */
    public Report(Result result, String label) {
        this.result = result;
        this.label = label;
    }
    
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
    
    public Map<String, Integer> aggregate() {
        return getResult().getComponents()
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }

    public Map<String, Integer> aggregate(Predicate<? super Item> filter) {
        return getResult().getComponents()
                .stream()
                .filter(filter)
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }
    
}
