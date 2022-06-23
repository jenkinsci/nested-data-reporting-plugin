package io.jenkins.plugins.reporter;

import java.util.Map;

public class ColorProvider {
    
    final static String DEFAULT_COLOR = "#9E9E9E";
    final Map<String, String> colors;
    
    public ColorProvider(final Map<String, String> colors) {
        this.colors = colors;
    }

    public String getColor(String id) {
        return colors.getOrDefault(id, DEFAULT_COLOR);
    }
    
    public Map<String, String> getColorMapping() {
        return colors;
    }
}
