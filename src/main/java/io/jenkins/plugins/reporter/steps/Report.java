package io.jenkins.plugins.reporter.steps;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.FormatMethod;
import edu.hm.hafner.util.PathUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.*;

public class Report implements Serializable {

    private static final long serialVersionUID = 302445084497230108L;
    
    private List<String> infoMessages;
    
    private List<String> errorMessages;

    private List<Report> subReports;
    
    private String id;
    
    private String name;

    private List<Item> items;
    
    private Map<String, String> colors;
    
    private String originReportFile;
    
    public Report() {
        this("-", "-", "-");
    }

    public Report(String id, String name) {
        this(id, name, "-");
    }
    
    public Report(String id, String name, String originReportFile) {
        this.infoMessages = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.subReports = new ArrayList<>();
        this.colors = new HashMap<>();
        this.items = new ArrayList<>();
        this.id = id;
        this.name = name;
        this.originReportFile = originReportFile;
    }

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

    public List<Report> getSubReports() {
        return subReports;
    }

    public void setSubReports(List<Report> subReports) {
        this.subReports = subReports;
    }

    public List<Item> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }

        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Map<String, String> getColors() {
        if (colors == null) {
            return Collections.emptyMap();
        }
        
        return colors;
    }

    public void setColors(Map<String, String> colors) {
        this.colors = colors;
    }
    
    public void add(Report report) {
        this.subReports.add(report);
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
    
}
