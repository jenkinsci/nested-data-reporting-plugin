package io.jenkins.plugins.reporter.steps;

import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.ReportScanner;
import io.jenkins.plugins.reporter.model.ColorPalette;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
// import java.nio.charset.StandardCharsets; // Not used, can be removed
// import java.nio.file.Files; // Not used, can be removed
import java.util.Arrays;
// import java.util.Collections; // Not used, can be removed
import java.util.List;
// import java.util.Map; // Not directly used as a variable type, but its methods are. Keep for clarity or remove if strict.
import java.util.regex.Pattern; // Added import

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ReportRecorderIntegrationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private Run<?, ?> mockRun;
    @Mock
    private TaskListener mockTaskListener;
    @Mock
    private Provider mockProvider;

    private FilePath workspace;

    @Before
    public void setUp() throws IOException, InterruptedException {
        MockitoAnnotations.openMocks(this);
        workspace = new FilePath(temporaryFolder.newFolder("workspace"));

        // Mock behavior for Provider
        // when(mockProvider.getName()).thenReturn("TestProvider"); // Old line - Removed
        when(mockProvider.getSymbolName()).thenReturn("testProvider"); // Correct line, kept
        
        // Basic mock for Run
        when(mockRun.getRootDir()).thenReturn(temporaryFolder.newFolder("runRootDir"));
    }

    private Report createDummyReportData(String reportIdName, List<String> itemIds) {
        Report report = new Report(reportIdName); // Sets the report's name
        report.setId(reportIdName); // Also use as ID for simplicity in test
        List<Item> items = new java.util.ArrayList<>();
        for (String itemId : itemIds) {
            Item item = new Item();
            item.setId(itemId);
            item.setName("Item " + itemId);
            item.addResult(itemId + "_data", 10); // Make sure Item.addResult is valid
            items.add(item);
        }
        report.setItems(items);
        return report;
    }

    @Test
    public void testColorPaletteAppliedThroughRecorderAndScanner() throws IOException, InterruptedException {
        // 1. Setup ReportRecorder
        ReportRecorder recorder = new ReportRecorder();
        recorder.setName("TestReport");
        
        // Use a real theme name
        String testThemeName = ColorPalette.Theme.RAINBOW.name(); 
        recorder.setColorPalette(testThemeName);

        List<String> itemIds = Arrays.asList("itemA", "itemB", "itemC");
        Report dummyProviderReport = createDummyReportData("test-report", itemIds); 
        
        when(mockProvider.scan(any(Run.class), any(FilePath.class), any(io.jenkins.plugins.reporter.util.LogHandler.class)))
           .thenReturn(dummyProviderReport);
            
        recorder.setProvider(mockProvider);

        // 2. Execute ReportRecorder's core logic (simplified from perform/record)
        // We are focusing on the path that involves ReportScanner
        // ReportRecorder.scan (private method) calls new ReportScanner(...).scan()
        // To test this integration, we can directly make a ReportScanner instance as the test does.
        ReportScanner scanner = new ReportScanner(mockRun, mockProvider, workspace, mockTaskListener, recorder.getColorPalette());
        Report resultReport = scanner.scan(); // This should apply the color palette

        // 3. Assertions
        assertNotNull(resultReport);
        assertNotNull(resultReport.getColors());
        assertFalse(resultReport.getColors().isEmpty());
        assertEquals(itemIds.size(), resultReport.getColors().size());

        // Verify that the colors match the RAINBOW theme
        // Accessing THEMES map which was made package-private
        String[] rainbowColors = io.jenkins.plugins.reporter.model.ColorPalette.THEMES.get(ColorPalette.Theme.RAINBOW);
        assertNotNull(rainbowColors);

        for (int i = 0; i < itemIds.size(); i++) {
            String itemId = itemIds.get(i);
            assertTrue("Report colors should contain ID: " + itemId, resultReport.getColors().containsKey(itemId));
            assertEquals("Color for " + itemId + " does not match RAINBOW theme", rainbowColors[i % rainbowColors.length], resultReport.getColors().get(itemId));
        }
    }
    
    @Test
    public void testDefaultRandomPaletteWhenNoThemeSpecified() throws IOException, InterruptedException {
        ReportRecorder recorder = new ReportRecorder();
        recorder.setName("TestReportDefaultTheme");
        // recorder.setColorPalette(null); // or empty string - this is the default for the field, or set explicitly for clarity
        // ColorPalette constructor defaults to RANDOM if themeName is null or empty.

        List<String> itemIds = Arrays.asList("itemX", "itemY");
        Report dummyProviderReport = createDummyReportData("default-theme-report", itemIds);

        when(mockProvider.scan(any(Run.class), any(FilePath.class), any(io.jenkins.plugins.reporter.util.LogHandler.class)))
            .thenReturn(dummyProviderReport);
        recorder.setProvider(mockProvider);

        ReportScanner scanner = new ReportScanner(mockRun, mockProvider, workspace, mockTaskListener, recorder.getColorPalette());
        Report resultReport = scanner.scan();

        assertNotNull(resultReport);
        assertNotNull(resultReport.getColors());
        assertFalse(resultReport.getColors().isEmpty());
        assertEquals(itemIds.size(), resultReport.getColors().size());

        // Check that colors are valid hex, as they should be random
        Pattern hexPattern = Pattern.compile("^#[0-9a-fA-F]{6}$");
        for (String itemId : itemIds) {
            assertTrue(resultReport.getColors().containsKey(itemId));
            String color = resultReport.getColors().get(itemId);
            assertNotNull(color);
            assertTrue("Color " + color + " for " + itemId + " should be a valid hex color", hexPattern.matcher(color).matches());
        }
    }
}
