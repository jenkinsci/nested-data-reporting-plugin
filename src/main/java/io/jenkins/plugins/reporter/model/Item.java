package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Json Model class, which represents an {@link Item}. 
 * An item always has an {@link Item#id} and a map of {@link Item#result}.
 * In addition, an {@link Item} can in turn contain a list of items.
 *
 * @author Simon Symhoven
 */
public class Item implements Serializable {
    
    private static final long serialVersionUID = -2800979294230808946L;

    @JsonProperty("id")
    private String id;
    
    LinkedHashMap<String, Integer> result;

    List<Item> items;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LinkedHashMap<String, Integer> getResult() {
        if (result != null) {
            return result;
        }
                
        return getItems()
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.summingInt(Map.Entry::getValue)));
    }

    public void setResult(LinkedHashMap<String, Integer> result) {
        this.result = result;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}