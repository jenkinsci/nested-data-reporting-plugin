package io.jenkins.plugins.reporter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.errorprone.annotations.FormatMethod;
import edu.hm.hafner.util.PathUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Report implements Serializable {

    private static final long serialVersionUID = 302445084497230108L;

    private static final String DEFAULT_COLOR = "#9E9E9E";
    
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
        this.subReports.addAll(report.getSubReports());
        this.infoMessages.addAll(report.getInfoMessages());
        this.infoMessages.addAll(report.getErrorMessages());
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

    @JsonIgnore
    public String getColor(String id) {
        String color = getColors().getOrDefault(id, DEFAULT_COLOR);

        if (!color.startsWith("#")) {
            try {
                return Palette.valueOf(color).getColor();
            } catch (IllegalArgumentException e) {
                return DEFAULT_COLOR;
            }
        }

        return color;
    }

    @JsonIgnore
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
    @JsonIgnore
    public LinkedHashMap<String, Integer> aggregate(List<Item> items) {
        return items
                .stream()
                .map(Item::getResult)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.summingInt(Map.Entry::getValue)));
    }

    @JsonIgnore
    public List<String> getColorIds() {
        if (aggregate().size() == 1) {
            return findItems(getItems()).stream().map(Item::getId).collect(Collectors.toList());
        }

        return new ArrayList<>(aggregate().keySet());
    }

    @JsonIgnore
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
    @JsonIgnore
    public LinkedHashMap<String, Integer> aggregate() {
        return aggregate(getItems());
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
