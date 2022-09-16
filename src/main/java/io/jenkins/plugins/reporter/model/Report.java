package io.jenkins.plugins.reporter.model;

import com.google.errorprone.annotations.FormatMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Report implements Serializable {

    private static final long serialVersionUID = 302445084497230108L;

    private static final String DEFAULT_COLOR = "#9E9E9E";
    
    private final List<String> infoMessages;
    
    private final List<String> errorMessages;
    
    private DisplayType displayType = DisplayType.ABSOLUTE;
    
    private List<Report> subReports;
    
    private String id;
    
    private String name;
    
    private List<Item> items;
    
    private Map<String, String> colors;
    
    public Report() {
        this("-");
    }
    
    public Report(String name) {
        this.infoMessages = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
        this.subReports = new ArrayList<>();
        this.colors = new HashMap<>();
        this.items = new ArrayList<>();
        this.name = name;
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

    public DisplayType getDisplayType() {
        return displayType;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
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
        
        if (StringUtils.isEmpty(id)) {
            setId(report.getId());
            logInfo("Add first report. Set ID='%s'", report.getId());
        }
        
        if (getId().equals(report.getId())) {
            this.subReports.add(report);
            this.infoMessages.addAll(report.getInfoMessages());
            this.errorMessages.addAll(report.getErrorMessages());
            addColors(report.getColors());
            addItems(report.getItems());
            logInfo("Successfully added report with ID='%s'", report.getId());
        } else {
            logError("Skip adding report because ID='%s' is different from parent ID='%s'.", 
                    report.getId(), getId());
        }
    }
    
    public List<String> getInfoMessages() {
        return this.infoMessages;
    }

    public List<String> getErrorMessages() {
        return this.errorMessages;
    }

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

    public boolean hasColors() {
        return this.colors != null && this.colors.size() > 0;
    }

    public boolean hasItems() {
        return this.items != null && this.items.size() > 0;
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

    public List<String> getColorIds() {
        if (aggregate().size() == 1) {
            return flattItems(getItems()).stream().map(Item::getId).collect(Collectors.toList());
        }

        return new ArrayList<>(aggregate().keySet());
    }

    private List<Item> flattItems(List<Item> items)
    {
        List<Item> flatten = new ArrayList<>();

        for (Item i: items) {
            if (i.hasItems()) {
                flatten.addAll(flattItems(i.getItems()));
            }

            flatten.add(i);
        }

        return flatten;
    }

    public Optional<Item> findItem(String id) {
        return findItem(id, items);
    }

    public Optional<Item> findItem(String id, List<Item> items) {
        if (items != null) {
            for (Item i: items) {
                if (i.getId().equals(id)) {
                    return Optional.of(i);
                } else {
                    Optional<Item> sub = findItem(id, i.getItems());
                    if (sub.isPresent()) {
                        return sub;
                    }
                }
            }
        }

        return Optional.empty();
    }


    /**
     * Aggregates the results of all items. The values are added together, grouped by key. 
     *
     * @return the aggregated result.
     */
 
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
    
    private void addColors(Map<String, String> colors) {
        setColors(Stream.concat(getColors().entrySet().stream(), colors.entrySet().stream()).collect(
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
    
    private void addItems(List<Item> itemsToAdd) {
        for (Item item : itemsToAdd) {
            Optional<Item> parent = this.items.stream().filter(i -> i.getId().equals(item.getId())).findAny();
            
            if (parent.isPresent()) {
                if (item.hasItems()) {
                    merge(item, item.getItems());
                }
            } else {
                logInfo("Add item wih ID='%s' to items.", item.getId());
                this.items.add(item);
            }
        }
    }
    
    private void merge(Item parentItem, List<Item> itemsToMerge) {
        for (Item item : itemsToMerge) {
            if (item.hasItems()) {
                merge(item, item.getItems());
            } else {
                Optional<Item> found = findItem(parentItem.getId(), items);
                
                if (found.isPresent()) {
                    logInfo("Add item with ID='%s' to parent item with ID='%s'.", item.getId(), found.get().getId());
                    Item parent = found.get();
                    parent.addItem(item);
                } else {
                    logError("No parent item found for ID='%s' in items.", parentItem.getId());
                }
            }
        }
    }
}
