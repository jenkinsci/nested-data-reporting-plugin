package io.jenkins.plugins.reporter.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple data class that manages a list of {@link Item} and a label add by the 
 * {@link io.jenkins.plugins.reporter.PublishReportStep}.
 *
 * @author Simon Symhoven
 */
public class Report implements Serializable {

    private static final long serialVersionUID = -4523053939010906220L;
    private final List<Item> items;
    private final String label;

    /**
     * Creates a new {@link Report}.
     * @param items
     * @param label
     */
    public Report(List<Item> items, String label) {
        this.items = items;
        this.label = label;
    }
    
    public Report() {
        this.label = "Data Report";
        this.items = Collections.emptyList();
    }
    public List<Item> getItems() {
        return items;
    }

    
    public String getLabel() {
        return label;
    }
    
    public Map<String, Integer> aggregate() {
        return getItems()
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
    }
    
    
}
