package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Json Model class, which represents an {@link Result}. 
 * Simple data class that manages a list of {@link Item} and the corresponding color mapping.
 *
 * @author Simon Symhoven
 */
public class Result implements Serializable {
    
    private static final long serialVersionUID = 7878818807240640969L;

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty(value = "id", required = true)
    private String id = String.valueOf(hashCode());

    @JsonProperty(value = "name", required = true)
    private String name = String.valueOf(hashCode());
    
    @JsonProperty(value = "items", required = true)
    private List<Item> items;
    
    @JsonProperty(value = "colors", required = true)
    private Map<String, String> colors;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
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
    
    public boolean hasColors() {
        return this.colors != null && this.colors.size() > 0;
    }
    /**
     * Aggregates the results of all items. The values are added together, grouped by key. 
     * 
     * @param items
     *              the items to aggregate the childs for.
     * @return the aggregated result.
     */
    public LinkedHashMap<String, Integer> aggregate(List<Item> items) {
        return items
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.summingInt(Map.Entry::getValue)));
    }
    
    public List<String> getIds() {
        if (aggregate().size() == 1) {
            return findItems(getItems()).stream().map(Item::getId).collect(Collectors.toList());
        }
        
        return new ArrayList<>(aggregate().keySet());
    }

    public List<Item> findItems(List<Item> items)
    {
        List<Item> flatten = new ArrayList<>();
        
        for (Item i: items) {
            if (i.hasItems()) {
                flatten.addAll(findItems(i.getItems()));
            }
            
            flatten.add(i);
        }

        return flatten;
    }
    
    /**
     * Aggregates the results of all items. The values are added together, grouped by key. 
     *
     * @return the aggregated result.
     */
    public LinkedHashMap<String, Integer> aggregate() {
        return aggregate(getItems());
    }
}
