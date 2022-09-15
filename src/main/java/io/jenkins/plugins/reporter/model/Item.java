package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jline.internal.Nullable;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @JsonProperty(value = "id", required = true)
    private String id;

    /**
     * @since 2.4.0
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "result", required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    LinkedHashMap<String, Integer> result;

    @Nullable
    @JsonProperty(value = "items", required = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<Item> items;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
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

    public boolean hasItems() {
        return !Objects.isNull(items) && !items.isEmpty();
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
    
    public void addItem(Item item) {
        this.items.add(item);
    }
}