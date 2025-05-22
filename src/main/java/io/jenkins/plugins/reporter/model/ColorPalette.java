package io.jenkins.plugins.reporter.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ColorPalette {

    public enum Theme {
        RANDOM,
        RAINBOW,
        BLUE_SPECTRA,
        SOLARIZED_LIGHT,
        SOLARIZED_DARK,
        GREYSCALE,
        DRACULA,
        MONOKAI,
        NORD,
        GRUVBOX_DARK,
        GRUVBOX_LIGHT,
        MATERIAL_DARK,
        MATERIAL_LIGHT,
        ONE_DARK,
        ONE_LIGHT,
        // TOMORROW_NIGHT, // Removed
        // TOMORROW, // Removed
        VIRIDIS,
        // PLASMA, // Removed
        TABLEAU_CLASSIC_10,
        CATPPUCCIN_MACCHIATO,
        EXCEL_OFFICE_DEFAULT, // Added
        EXCEL_BLUE_II, // Added
        EXCEL_GREEN_II, // Added
        EXCEL_RED_VIOLET_II // Added
    }

    static final Map<Theme, String[]> THEMES; // Changed from private to package-private

    static {
        Map<Theme, String[]> map = new HashMap<>();
        map.put(Theme.RAINBOW, new String[]{"#FF0000", "#FF7F00", "#FFFF00", "#00FF00", "#0000FF", "#4B0082", "#9400D3"});
        map.put(Theme.BLUE_SPECTRA, new String[]{"#0D47A1", "#1565C0", "#1976D2", "#1E88E5", "#2196F3", "#42A5F5", "#64B5F6", "#90CAF9"});
        map.put(Theme.SOLARIZED_LIGHT, new String[]{"#b58900", "#cb4b16", "#dc322f", "#d33682", "#6c71c4", "#268bd2", "#2aa198", "#859900"});
        map.put(Theme.SOLARIZED_DARK, new String[]{"#586e75", "#dc322f", "#d33682", "#6c71c4", "#268bd2", "#2aa198", "#859900", "#b58900"});
        map.put(Theme.GREYSCALE, new String[]{"#2F4F4F", "#556B2F", "#A9A9A9", "#D3D3D3", "#F5F5F5"});
        
        // Curated themes (some were removed from here later)
        map.put(Theme.DRACULA, new String[]{"#FF79C6", "#50FA7B", "#F1FA8C", "#BD93F9", "#8BE9FD", "#FFB86C", "#FF5555", "#6272A4"});
        map.put(Theme.MONOKAI, new String[]{"#F92672", "#A6E22E", "#FD971F", "#E6DB74", "#66D9EF", "#AE81FF"});
        map.put(Theme.NORD, new String[]{"#BF616A", "#A3BE8C", "#EBCB8B", "#81A1C1", "#B48EAD", "#88C0D0", "#D08770"});
        map.put(Theme.GRUVBOX_DARK, new String[]{"#FB4934", "#B8BB26", "#FABD2F", "#83A598", "#D3869B", "#8EC07C", "#FE8019"});
        map.put(Theme.GRUVBOX_LIGHT, new String[]{"#CC241D", "#98971A", "#D79921", "#458588", "#B16286", "#689D6A", "#D65D0E"});
        map.put(Theme.MATERIAL_DARK, new String[]{"#F06292", "#81C784", "#FFD54F", "#7986CB", "#4FC3F7", "#FF8A65", "#A1887F", "#90A4AE"});
        map.put(Theme.MATERIAL_LIGHT, new String[]{"#E91E63", "#4CAF50", "#FFC107", "#3F51B5", "#03A9F4", "#FF5722", "#795548", "#607D8B"});
        map.put(Theme.ONE_DARK, new String[]{"#E06C75", "#98C379", "#E5C07B", "#61AFEF", "#C678DD", "#56B6C2"});
        map.put(Theme.ONE_LIGHT, new String[]{"#E45649", "#50A14F", "#C18401", "#4078F2", "#A626A4", "#0184BC"});
        // map.put(Theme.TOMORROW_NIGHT, new String[]{"#CC6666", "#B5BD68", "#F0C674", "#81A2BE", "#B294BB", "#8ABEB7", "#DE935F"}); // Removed
        // map.put(Theme.TOMORROW, new String[]{"#C82829", "#718C00", "#EAB700", "#4271AE", "#8959A8", "#3E999F", "#D6700C"}); // Removed
        map.put(Theme.VIRIDIS, new String[]{"#440154", "#414487", "#2A788E", "#22A884", "#7AD151", "#FDE725"});
        // map.put(Theme.PLASMA, new String[]{"#0D0887", "#6A00A8", "#B12A90", "#E16462", "#FCA636", "#F0F921"}); // Removed
        map.put(Theme.TABLEAU_CLASSIC_10, new String[]{"#1F77B4", "#FF7F0E", "#2CA02C", "#D62728", "#9467BD", "#8C564B", "#E377C2", "#7F7F7F", "#BCBD22", "#17BECF"});
        map.put(Theme.CATPPUCCIN_MACCHIATO, new String[]{"#F0C6C6", "#A6D189", "#E5C890", "#8CAAEE", "#C6A0F6", "#81C8BE", "#F4B8A9"});

        // Adding new Excel themes
        map.put(Theme.EXCEL_OFFICE_DEFAULT, new String[]{"#4472C4", "#ED7D31", "#A5A5A5", "#FFC000", "#5B9BD5", "#70AD47"});
        map.put(Theme.EXCEL_BLUE_II, new String[]{"#2F5597", "#5A89C8", "#8FB4DB", "#4BACC6", "#77C9D9", "#A9A9A9"});
        map.put(Theme.EXCEL_GREEN_II, new String[]{"#548235", "#70AD47", "#A9D18E", "#C5E0B4", "#8497B0", "#BF8F00"});
        map.put(Theme.EXCEL_RED_VIOLET_II, new String[]{"#C00000", "#900000", "#7030A0", "#A98EDA", "#E97EBB", "#BDBDBD"});
        
        THEMES = Collections.unmodifiableMap(map);
    }

    private final List<String> ids;
    private final String themeName;

    public ColorPalette(List<String> ids, String themeName) {
        this.ids = ids;
        if (themeName == null || themeName.isEmpty()) {
            this.themeName = Theme.RANDOM.name();
        } else {
            this.themeName = themeName;
        }
    }

    public Map<String, String> generatePalette() {
        Map<String, String> colors = new HashMap<>();
        Theme selectedTheme;
        try {
            selectedTheme = Theme.valueOf(this.themeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            selectedTheme = Theme.RANDOM;
        }

        if (selectedTheme == Theme.RANDOM) {
            ids.forEach(id -> {
                int rand_num = ThreadLocalRandom.current().nextInt(0xffffff + 1);
                String color = String.format("#%06x", rand_num);
                colors.put(id, color);
            });
        } else {
            String[] themeColors = THEMES.get(selectedTheme);
            if (themeColors != null && themeColors.length > 0) {
                for (int i = 0; i < ids.size(); i++) {
                    colors.put(ids.get(i), themeColors[i % themeColors.length]);
                }
            } else {
                // Fallback to random if theme colors are missing (should not happen with enum keys)
                 ids.forEach(id -> {
                    int rand_num = ThreadLocalRandom.current().nextInt(0xffffff + 1);
                    String color = String.format("#%06x", rand_num);
                    colors.put(id, color);
                });
            }
        }
        return colors;
    }

    public static List<String> getAvailableThemes() {
        return Arrays.stream(Theme.values())
                .map(Theme::name)
                .collect(Collectors.toList());
    }

    public static int getDefinedColorCount(Theme theme) {
        if (theme != null && theme != Theme.RANDOM && THEMES.containsKey(theme)) {
            String[] colors = THEMES.get(theme);
            return colors != null ? colors.length : 0;
        }
        return 0; 
    }
}
