package io.jenkins.plugins.reporter.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ColorPalette {
    
    private final List<String> ids;
    
    public ColorPalette(List<String> ids) {
        this.ids = ids;
    }
    
    public Map<String, String> generatePalette() {
        
        Map<String, String> colors = new HashMap<>();
        
        ids.forEach(id -> {
            int rand_num = ThreadLocalRandom.current().nextInt(0xffffff + 1);
            String color = String.format("#%06x", rand_num);

            colors.put(id, color);
        });
        
        return colors;
    }
}
