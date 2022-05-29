package io.jenkins.plugins.reporter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Report implements Serializable {

    private static final long serialVersionUID = -4523053939010906220L;
    private String csv;
    private String label;
    private List<Asset> assets;
    
    public Report() {
        this.assets = new ArrayList<>();
    }
    
    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }

    public String getCsv() {
        return csv;
    }

    public void setCsv(String csv) {
        this.csv = csv;
    }
    
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    
    public int getTotalAccurate() {
        return assets.stream().mapToInt(Asset::getAccurate).sum();
    }

    public int getTotalManually() {
        return assets.stream().mapToInt(Asset::getManually).sum();
    }

    public int getTotalIncorrect() {
        return assets.stream().mapToInt(Asset::getIncorrect).sum();
    }
    
    public int getTotal() {
        return getTotalAccurate() + getTotalManually() + getTotalIncorrect();
    }
    
}
