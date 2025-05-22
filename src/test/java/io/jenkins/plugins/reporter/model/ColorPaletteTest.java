package io.jenkins.plugins.reporter.model;

import org.junit.Test; // Or org.junit.jupiter.api.Test if using JUnit 5
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*; // Or static org.junit.jupiter.api.Assertions.* for JUnit 5

public class ColorPaletteTest {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9a-fA-F]{6}$");

    @Test
    public void testGetAvailableThemes() {
        List<String> availableThemes = ColorPalette.getAvailableThemes();
        assertNotNull(availableThemes);
        assertFalse(availableThemes.isEmpty());
        
        // Check for a few expected themes
        assertTrue(availableThemes.contains(ColorPalette.Theme.RAINBOW.name()));
        assertTrue(availableThemes.contains(ColorPalette.Theme.SOLARIZED_DARK.name()));
        assertTrue(availableThemes.contains(ColorPalette.Theme.RANDOM.name()));
        
        // Check for a few new themes
        assertTrue(availableThemes.contains(ColorPalette.Theme.DRACULA.name())); // Kept
        assertTrue(availableThemes.contains(ColorPalette.Theme.NORD.name())); // Kept
        assertTrue(availableThemes.contains(ColorPalette.Theme.TABLEAU_CLASSIC_10.name())); // Kept
        assertTrue(availableThemes.contains(ColorPalette.Theme.CATPPUCCIN_MACCHIATO.name())); // Kept

        // Check for new Excel themes
        assertTrue(availableThemes.contains(ColorPalette.Theme.EXCEL_OFFICE_DEFAULT.name()));
        assertTrue(availableThemes.contains(ColorPalette.Theme.EXCEL_BLUE_II.name()));
        
        // Check for removed themes (should not be present)
        // Assuming ColorPalette.Theme still has them commented out, direct name() calls would fail compilation if fully removed.
        // If they are fully removed from enum, these lines are not needed / would not compile.
        // Based on previous step, they are commented out in enum, so direct .name() calls are not possible.
        // We will check against the list of strings.
        assertFalse("Theme PLASMA should be removed", availableThemes.contains("PLASMA"));
        assertFalse("Theme TOMORROW should be removed", availableThemes.contains("TOMORROW"));
        assertFalse("Theme TOMORROW_NIGHT should be removed", availableThemes.contains("TOMORROW_NIGHT"));

        // Verify total count of themes
        // Original 6 (RANDOM + 5 predefined)
        // Added 15 = 21
        // Removed 3 = 18
        // Added 4 (Excel) = 22
        assertEquals("Total number of available themes should be 22", 22, availableThemes.size());
        assertEquals("Total number of enum constants in Theme should be 22", 22, ColorPalette.Theme.values().length);
    }

    @Test
    public void testGeneratePaletteRandom() {
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        ColorPalette randomPalette = new ColorPalette(ids, ColorPalette.Theme.RANDOM.name());
        Map<String, String> colors = randomPalette.generatePalette();

        assertNotNull(colors);
        assertEquals(ids.size(), colors.size());
        for (String id : ids) {
            assertTrue(colors.containsKey(id));
            assertNotNull(colors.get(id));
            assertTrue("Color " + colors.get(id) + " should be a valid hex color", HEX_COLOR_PATTERN.matcher(colors.get(id)).matches());
        }
    }
    
    @Test
    public void testGeneratePaletteRandomWithNullTheme() {
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        ColorPalette randomPalette = new ColorPalette(ids, null); // Null theme should default to RANDOM
        Map<String, String> colors = randomPalette.generatePalette();

        assertNotNull(colors);
        assertEquals(ids.size(), colors.size());
        for (String id : ids) {
            assertTrue(colors.containsKey(id));
            assertNotNull(colors.get(id));
            assertTrue("Color " + colors.get(id) + " should be a valid hex color", HEX_COLOR_PATTERN.matcher(colors.get(id)).matches());
        }
    }

    @Test
    public void testGeneratePaletteRainbow() {
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        ColorPalette rainbowPalette = new ColorPalette(ids, ColorPalette.Theme.RAINBOW.name());
        Map<String, String> colors = rainbowPalette.generatePalette();

        assertNotNull(colors);
        assertEquals(ids.size(), colors.size());
        
        String[] rainbowColors = ColorPalette.THEMES.get(ColorPalette.Theme.RAINBOW);
        assertNotNull(rainbowColors);

        assertEquals(rainbowColors[0], colors.get("id1"));
        assertEquals(rainbowColors[1], colors.get("id2"));
        assertEquals(rainbowColors[2], colors.get("id3"));
    }

    @Test
    public void testGeneratePaletteThemeColorCycling() {
        // Use RAINBOW which has 7 colors
        List<String> ids = Arrays.asList("id1", "id2", "id3", "id4", "id5", "id6", "id7", "id8", "id9");
        ColorPalette themedPalette = new ColorPalette(ids, ColorPalette.Theme.RAINBOW.name());
        Map<String, String> colors = themedPalette.generatePalette();

        assertNotNull(colors);
        assertEquals(ids.size(), colors.size());

        String[] themeColors = ColorPalette.THEMES.get(ColorPalette.Theme.RAINBOW);
        assertNotNull(themeColors);

        assertEquals(themeColors[0], colors.get("id1"));
        assertEquals(themeColors[6], colors.get("id7"));
        assertEquals(themeColors[0], colors.get("id8")); // Should cycle back to the first color
        assertEquals(themeColors[1], colors.get("id9")); // Should cycle to the second color
    }
    
    @Test
    public void testAllPredefinedThemesGenerateValidColors() {
        List<String> ids = Arrays.asList("item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10");
        for (ColorPalette.Theme themeEnum : ColorPalette.Theme.values()) {
            if (themeEnum == ColorPalette.Theme.RANDOM) continue; // Skip RANDOM for this specific test logic

            ColorPalette palette = new ColorPalette(ids, themeEnum.name());
            Map<String, String> colorMap = palette.generatePalette();

            assertNotNull("Color map should not be null for theme: " + themeEnum.name(), colorMap);
            assertEquals("Color map size should match ID size for theme: " + themeEnum.name(), ids.size(), colorMap.size());

            String[] themeColors = ColorPalette.THEMES.get(themeEnum);
            assertNotNull("Theme colors definition missing for: " + themeEnum.name(), themeColors);
            assertTrue("Theme " + themeEnum.name() + " has no colors defined", themeColors.length > 0);
            
            Set<String> uniqueGeneratedColors = new HashSet<>();
            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i);
                assertTrue("ID " + id + " missing in color map for theme: " + themeEnum.name(), colorMap.containsKey(id));
                String colorValue = colorMap.get(id);
                assertNotNull("Color value is null for ID " + id + " in theme: " + themeEnum.name(), colorValue);
                assertTrue("Color " + colorValue + " for theme " + themeEnum.name() + " should be a valid hex color", HEX_COLOR_PATTERN.matcher(colorValue).matches());
                
                // Check if it's one of the theme's defined colors
                assertEquals("Generated color for " + id + " not from theme " + themeEnum.name(), themeColors[i % themeColors.length], colorValue);
                uniqueGeneratedColors.add(colorValue);
            }
            
            // Number of unique colors should be at most the number of colors in the theme definition
            assertTrue("More unique colors generated than defined for theme " + themeEnum.name(), uniqueGeneratedColors.size() <= themeColors.length);
            // Or exactly if ids.size() >= themeColors.length and ids are enough to use all colors
            if (ids.size() >= themeColors.length) {
                 assertEquals("Not all defined colors were used for theme: " + themeEnum.name(), themeColors.length, uniqueGeneratedColors.size());   
            } else {
                 assertEquals("Number of unique colors generated does not match number of ids for theme: " + themeEnum.name(), ids.size(), uniqueGeneratedColors.size());   
            }
        }
    }

    @Test
    public void testGetDefinedColorCount() {
        assertEquals(7, ColorPalette.getDefinedColorCount(ColorPalette.Theme.RAINBOW));
        assertEquals(6, ColorPalette.getDefinedColorCount(ColorPalette.Theme.EXCEL_OFFICE_DEFAULT));
        assertEquals(10, ColorPalette.getDefinedColorCount(ColorPalette.Theme.TABLEAU_CLASSIC_10)); 
        assertEquals(8, ColorPalette.getDefinedColorCount(ColorPalette.Theme.DRACULA)); 
        
        // Test for a theme that was kept and has a different count
        String[] nordColors = ColorPalette.THEMES.get(ColorPalette.Theme.NORD); // NORD has 7
        assertNotNull("NORD theme colors should not be null", nordColors); 
        assertEquals(nordColors.length, ColorPalette.getDefinedColorCount(ColorPalette.Theme.NORD));

        // Test for another Excel theme
        assertEquals(6, ColorPalette.getDefinedColorCount(ColorPalette.Theme.EXCEL_BLUE_II));

        assertEquals(0, ColorPalette.getDefinedColorCount(ColorPalette.Theme.RANDOM));
        assertEquals(0, ColorPalette.getDefinedColorCount(null));

        // Test removed themes - this requires them to be fully removed from enum to pass value,
        // or for getDefinedColorCount to handle non-existence in THEMES map gracefully.
        // If TOMORROW is still an enum constant (e.g. commented out in map but not enum),
        // then getDefinedColorCount should return 0.
        // If TOMORROW is fully removed from enum, this test line would be a compile error.
        // Assuming they are fully removed from enum for this test to be meaningful for getDefinedColorCount.
        // However, based on previous step, they are only commented out in enum, making direct ref impossible.
        // So, we can't directly test Theme.TOMORROW.
        // The method getDefinedColorCount takes Theme enum, if the enum variant doesn't exist, we can't pass it.
        // If a theme exists in enum but not in THEMES map (e.g. RANDOM), it returns 0, which is correct.
    }
}
