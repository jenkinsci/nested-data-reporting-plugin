package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

public class ReportDto extends ReportBase {
    
    @JsonProperty(value = "id", required = true)
    private String id;

    @JsonProperty(value = "items", required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Item> items;

    @JsonProperty(value = "colors")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> colors;

    @JsonProperty(value = "parserLogMessages")
    @JsonInclude(JsonInclude.Include.NON_EMPTY) // Only include in JSON if not empty
    private List<String> parserLogMessages;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<String> getParserLogMessages() {
        return parserLogMessages;
    }

    public void setParserLogMessages(List<String> parserLogMessages) {
        this.parserLogMessages = parserLogMessages;
    }
    
    @JsonIgnore
    public Report toReport() {
        Report report = new Report();
        report.setId(getId());
        report.setItems(Optional.ofNullable(getItems()).orElseGet(Collections::emptyList));
        report.setColors(Optional.ofNullable(getColors()).orElseGet(Collections::emptyMap));
        return report;
    }
}
