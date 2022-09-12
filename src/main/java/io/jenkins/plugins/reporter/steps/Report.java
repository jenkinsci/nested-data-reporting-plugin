package io.jenkins.plugins.reporter.steps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.FormatMethod;
import edu.hm.hafner.util.PathUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Report implements Serializable {

    private static final long serialVersionUID = 302445084497230108L;
    
    private List<String> infoMessages;
    
    private List<String> errorMessages;

    private String originReportFile;

    @JsonProperty(value = "id", required = true)
    private String id;

    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonProperty(value = "items", required = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Item> items;

    @JsonProperty(value = "colors")
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    public void setName(String name) {
        this.name = name;
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
    
    public Report() {
        this.infoMessages = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.originReportFile = "-";
    }

    public List<String> getInfoMessages() {
        return this.infoMessages;
    }

    public List<String> getErrorMessages() {
        return this.errorMessages;
    }

    public String getOriginReportFile() {
        return this.originReportFile;
    }

    public void setOriginReportFile(String originReportFile) {
        this.originReportFile = (new PathUtil()).getAbsolutePath(originReportFile);
    }
    
    @FormatMethod
    public void logInfo(String format, Object... args) {
        this.infoMessages.add(String.format(format, args));
    }

    @FormatMethod
    public void logError(String format, Object... args) {
        this.errorMessages.add(String.format(format, args));
    }

    @FormatMethod
    public void logException(Exception exception, String format, Object... args) {
        this.logError(format, args);
        Collections.addAll(this.errorMessages, ExceptionUtils.getRootCauseStackTrace(exception));
    }

    public Report add(Report report) {
        setId(report.getId());
        return this;
    }
    
    
}
