package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Json Model class, which represents an {@link Result}. 
 * Simple data class that manages a list of {@link Item} and the corresponding color mapping.
 *
 * @author Simon Symhoven
 */
public class Result implements Serializable {
    
    private static final long serialVersionUID = 7878818807240640969L;
            
    @JsonProperty(value = "items", required = true)
    private List<Item> items;
    
    @JsonProperty(value = "colors", required = true)
    private Map<String, String> colors;
    
    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
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
     * @param filter
     *          the filter to evaluate on the result.
     * @return the aggregated result.
     */
    public LinkedHashMap<String, Integer> aggregate(Predicate<? super Item> filter) {
        return getItems()
                .stream()
                .filter(filter)
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.summingInt(Map.Entry::getValue)));
    }

    /**
     * Aggregates the results of all items. The values are added together, grouped by key. 
     *
     * @return the aggregated result.
     */
    public LinkedHashMap<String, Integer> aggregate() {
        return aggregate(item -> {return true;});
    }
}
