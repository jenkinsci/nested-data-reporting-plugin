package io.jenkins.plugins.reporter.model;

public enum Palette {
    YELLOW("#FFF59D"),
    LIME("#E6EE9C"),
    GREEN("#A5D6A7"),
    BLUE("#90CAF9"),
    TEAL("#80CBC4"),
    ORANGE("#FFCC80"),
    INDIGO("#9FA8DA"),
    PURPLE("#CE93D8"),
    RED("#EF9A9A"),
    BROWN("#BCAAA4"),
    GRAY("#D0D0D0"),
    WHITE("#FFFFFF");

    private final String color;

    private Palette(String color) {
        this.color = color;
    }

    public String getColor() {
        return this.color;
    }
}


