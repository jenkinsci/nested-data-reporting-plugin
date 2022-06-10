package io.jenkins.plugins.reporter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple data class that manages a list of {@link Asset}, a label and the csv of the build.
 *
 * @author Simon Symhoven
 */
public class Report implements Serializable {

    private static final long serialVersionUID = -4523053939010906220L;
    private String csv;
    private String label;
    private List<Asset> assets;

    /**
     * Creates a new {@link Report} with an empty list of {@link Asset}.
     */
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

    /**
     * Calculate the sum of accurate over all assets.
     * 
     * @return
     *         sum of accurate parts over all assets as int.         
     */
    public int getTotalAccurate() {
        return assets.stream().mapToInt(Asset::getAccurate).sum();
    }

    /**
     * Calculate the sum of manually over all assets.
     *
     * @return
     *         sum of manually parts over all assets as int.         
     */
    public int getTotalManually() {
        return assets.stream().mapToInt(Asset::getManually).sum();
    }

    /**
     * Calculate the sum of incorrect over all assets.
     *
     * @return
     *         sum of incorrect parts over all assets as int.         
     */
    public int getTotalIncorrect() {
        return assets.stream().mapToInt(Asset::getIncorrect).sum();
    }

    /**
     * Calculate the total sum for correct, manually and incorrect parts over all assets.
     *
     * @return
     *         sum of all parts over all assets as int.         
     */
    public int getTotal() {
        return getTotalAccurate() + getTotalManually() + getTotalIncorrect();
    }
    
}
