package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Json Model class, which represents an {@link Result}. 
 * Simple data class that manages a list of {@link Item} and the corresponding color mapping.
 *
 * @author Simon Symhoven
 */
public class Result implements Serializable {

    private static final long serialVersionUID = 7878818807240640969L;
            
    @JsonProperty("components")
    private List<Item> components;
    
    private Map<String, String> colors;
    
    public List<Item> getComponents() {
        return components;
    }

    public void setComponents(List<Item> components) {
        this.components = components;
    }

    public Map<String, String> getColors() {
        return colors;
    }

    public void setColors(Map<String, String> colors) {
        this.colors = colors;
    }

    /**
     * Aggregates the results of all items. The values are added together, grouped by key. 
     * 
     * @return the aggregated result.
     */
    public LinkedHashMap<String, Integer> aggregate() {
        return getComponents()
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.summingInt(Map.Entry::getValue)));
    }
}
