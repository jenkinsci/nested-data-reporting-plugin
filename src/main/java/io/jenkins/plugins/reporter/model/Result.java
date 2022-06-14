package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Result implements Serializable {
    
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
}
