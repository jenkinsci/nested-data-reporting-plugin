package io.jenkins.plugins.reporter;

import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import io.jenkins.plugins.reporter.model.DisplayType;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.model.Report;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import io.jenkins.plugins.reporter.ReportAction; // Added import
import io.jenkins.plugins.reporter.ReportResult; // Added import

class ReportScannerTest {

    @Mock
    private Run<?, ?> currentRun;

    @Mock
    private Run<?, ?> previousSuccessfulRun;

    @Mock
    private TaskListener taskListener;

    @Mock
    private Provider provider;
    
    @Mock
    private FilePath workspace;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private ReportScanner reportScanner;
    private Report currentReport;
    private Report previousReport;
    private PrintStream printStream;
    private ByteArrayOutputStream baos;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currentReport = new Report();
        currentReport.setId("test-report");
        currentReport.setDisplayType(DisplayType.DIFF); // Assuming DIFF display type for these tests

        previousReport = new Report();
        previousReport.setId("test-report");
        previousReport.setDisplayType(DisplayType.ABSOLUTE); // Previous report could be absolute

        // Use a spy for the currentRun to allow mocking of its final methods like getPreviousSuccessfulBuild()
        // currentRun = spy(Run.class) ; // This would require a concrete class or more complex setup.
                                     // Sticking to doReturn for now as it's simpler with existing @Mock.
        reportScanner = new ReportScanner(currentRun, provider, workspace, taskListener);
        
        baos = new ByteArrayOutputStream();
        printStream = new PrintStream(baos);
        when(taskListener.getLogger()).thenReturn(printStream);
    }

    private Item createItem(String id, String name, LinkedHashMap<String, Integer> results, List<Item> nestedItems) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setResult(results);
        if (nestedItems != null) {
            item.setItems(nestedItems);
        }
        return item;
    }
    
    private LinkedHashMap<String, Integer> results(Object... kv) {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], (Integer) kv[i + 1]);
        }
        return map;
    }

    @Test
    void testProcessDiff_withPreviousSuccessfulBuild_calculatesCorrectDiffs() {
        // Setup current report items
        Item currentItem1 = createItem("id1", "Item 1", results("passed", 10, "failed", 2, "new", 5), null);
        Item currentItem2NestedChild = createItem("id2.1", "Nested Item 2.1", results("passed", 7), null);
        Item currentItem2 = createItem("id2", "Item 2", results("skipped", 3), Collections.singletonList(currentItem2NestedChild));
        currentReport.setItems(Arrays.asList(currentItem1, currentItem2));

        // Setup previous report items
        Item prevItem1 = createItem("id1", "Item 1", results("passed", 8, "failed", 3, "old", 2), null); // "old" key only in prev
        Item prevItem2NestedChild = createItem("id2.1", "Nested Item 2.1", results("passed", 5, "flaky", 1), null);
        Item prevItem2 = createItem("id2", "Item 2", results("skipped", 3), Collections.singletonList(prevItem2NestedChild));
        previousReport.setItems(Arrays.asList(prevItem1, prevItem2));

        ReportAction mockReportAction = mock(ReportAction.class);
        ReportResult mockReportResult = mock(ReportResult.class);
        when(mockReportAction.getResult()).thenReturn(mockReportResult);
        when(mockReportResult.getReport()).thenReturn(previousReport);
        
        doReturn(previousSuccessfulRun).when(currentRun).getPreviousSuccessfulBuild();
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getActions(ReportAction.class)).thenReturn(Collections.singletonList(mockReportAction)); // Use mockReportAction

        Report processedReport = reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        assertThat(processedReport).isSameAs(currentReport);

        // Assertions for Item 1
        Item processedItem1 = processedReport.getItems().stream().filter(i -> i.getId().equals("id1")).findFirst().orElse(null);
        assertThat(processedItem1).isNotNull();
        assertThat(processedItem1.getResult())
                .containsEntry("passed", 2)    // 10 - 8
                .containsEntry("failed", -1)   // 2 - 3
                .containsEntry("new", 5)       // 5 - 0
                .containsEntry("old", -2);     // 0 - 2

        // Assertions for Item 2 (top-level, its own results should be empty or diffed if it had direct results)
        Item processedItem2 = processedReport.getItems().stream().filter(i -> i.getId().equals("id2")).findFirst().orElse(null);
        assertThat(processedItem2).isNotNull();
         assertThat(processedItem2.getResult())
                .containsEntry("skipped", 0); // 3 - 3

        // Assertions for Nested Item 2.1
        assertThat(processedItem2.getItems()).hasSize(1);
        Item processedNestedItem21 = processedItem2.getItems().get(0);
        assertThat(processedNestedItem21.getId()).isEqualTo("id2.1");
        assertThat(processedNestedItem21.getResult())
                .containsEntry("passed", 2)    // 7 - 5
                .containsEntry("flaky", -1);   // 0 - 1
        
        printStream.flush(); // Ensure all output is written
        String logOutput = baos.toString();
        assertThat(logOutput).contains("Previous successful report found: test-report. Calculating diff.");
    }

    @Test
    void testProcessDiff_noPreviousSuccessfulBuild_returnsCurrentValues() {
        Item currentItem1 = createItem("id1", "Item 1", results("passed", 10), null);
        currentReport.setItems(Collections.singletonList(currentItem1));

        doReturn(null).when(currentRun).getPreviousSuccessfulBuild();

        Report processedReport = reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        assertThat(processedReport.getItems().get(0).getResult()).containsEntry("passed", 10);
        printStream.flush();
        String logOutput_noPrevBuild = baos.toString(); // Renamed to avoid conflict
        assertThat(logOutput_noPrevBuild).contains("No previous successful report found. Current values will be displayed as is.");
    }
    
    @Test
    void testProcessDiff_previousSuccessfulBuildExists_butNoMatchingReportAction() {
        Item currentItem1 = createItem("id1", "Item 1", results("passed", 10), null);
        currentReport.setItems(Collections.singletonList(currentItem1));

        doReturn(previousSuccessfulRun).when(currentRun).getPreviousSuccessfulBuild();
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getActions(ReportAction.class)).thenReturn(Collections.emptyList()); // No matching action

        Report processedReport = reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        assertThat(processedReport.getItems().get(0).getResult()).containsEntry("passed", 10);
        printStream.flush();
        String logOutput_noReportAction = baos.toString(); // Renamed to avoid conflict
        assertThat(logOutput_noReportAction).contains("No previous successful report found. Current values will be displayed as is.");
    }

    @Test
    void testProcessDiff_nestedItems_calculatesCorrectDiffs() {
        // This is largely covered by testProcessDiff_withPreviousSuccessfulBuild_calculatesCorrectDiffs
        // but we can add a more focused nested test if needed.
        // For now, relying on the coverage from the main test.
        // If specific complex nesting scenarios arise, they can be added here.
        Item currentChild = createItem("c1", "Child", results("val", 10), null);
        Item currentParent = createItem("p1", "Parent", results(), Collections.singletonList(currentChild));
        currentReport.setItems(Collections.singletonList(currentParent));

        Item prevChild = createItem("c1", "Child", results("val", 7), null);
        Item prevParent = createItem("p1", "Parent", results(), Collections.singletonList(prevChild));
        previousReport.setItems(Collections.singletonList(prevParent));
        
        ReportAction mockReportAction = mock(ReportAction.class);
        ReportResult mockReportResult = mock(ReportResult.class);
        when(mockReportAction.getResult()).thenReturn(mockReportResult);
        when(mockReportResult.getReport()).thenReturn(previousReport);

        doReturn(previousSuccessfulRun).when(currentRun).getPreviousSuccessfulBuild();
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getActions(ReportAction.class)).thenReturn(Collections.singletonList(mockReportAction));

        reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        assertThat(currentParent.getItems().get(0).getResult()).containsEntry("val", 3); // 10 - 7
    }

    @Test
    void testProcessDiff_itemInCurrent_notInPrevious() {
        Item currentItemOnly = createItem("unique_id", "Unique Item", results("new_data", 100), null);
        currentReport.setItems(Collections.singletonList(currentItemOnly));

        // Previous report is empty or does not contain 'unique_id'
        previousReport.setItems(Collections.emptyList()); 
        
        ReportAction mockReportAction = mock(ReportAction.class);
        ReportResult mockReportResult = mock(ReportResult.class);
        when(mockReportAction.getResult()).thenReturn(mockReportResult);
        when(mockReportResult.getReport()).thenReturn(previousReport);

        doReturn(previousSuccessfulRun).when(currentRun).getPreviousSuccessfulBuild();
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getActions(ReportAction.class)).thenReturn(Collections.singletonList(mockReportAction));

        reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        Item processedItem = currentReport.getItems().get(0);
        assertThat(processedItem.getResult()).containsEntry("new_data", 100); // Values remain as is
        
        // Check log message
        printStream.flush();
        String logOutput = baos.toString();
        assertThat(logOutput).contains("Item with ID 'unique_id' (name: 'Unique Item') not found in previous successful report. Current values will be used as diff.");
    }
    
    @Test
    void testProcessDiff_itemInPrevious_notInCurrent() {
        // Current report is empty
        currentReport.setItems(Collections.emptyList());

        // Previous report had an item
        Item prevItemOnly = createItem("old_id", "Old Item", results("old_data", 50), null);
        previousReport.setItems(Collections.singletonList(prevItemOnly));

        ReportAction mockReportAction = mock(ReportAction.class);
        ReportResult mockReportResult = mock(ReportResult.class);
        when(mockReportAction.getResult()).thenReturn(mockReportResult);
        when(mockReportResult.getReport()).thenReturn(previousReport);

        doReturn(previousSuccessfulRun).when(currentRun).getPreviousSuccessfulBuild();
        when(previousSuccessfulRun.getResult()).thenReturn(Result.SUCCESS);
        when(previousSuccessfulRun.getActions(ReportAction.class)).thenReturn(Collections.singletonList(mockReportAction));

        Report processedReport = reportScanner.processDiffReport(currentReport, currentRun, taskListener);

        // Assert that the processed report is still empty, as the diff only processes current items
        assertThat(processedReport.getItems()).isEmpty();
        printStream.flush();
        String logOutput_itemInPrevOnly = baos.toString(); // Renamed to avoid conflict
        assertThat(logOutput_itemInPrevOnly).contains("Previous successful report found: test-report. Calculating diff.");
        // No errors should occur, and no "Item with ID 'old_id' not found" message for items *only* in previous.
    }
}
