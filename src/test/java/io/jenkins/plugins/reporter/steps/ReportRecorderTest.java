package io.jenkins.plugins.reporter.steps;

import io.jenkins.plugins.reporter.model.Provider;
import io.jenkins.plugins.reporter.provider.Json; // Using Json as a concrete Provider
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.List;
import java.io.PrintStream;
import java.io.IOException;

// Mockito imports - assuming Mockito is available in the project
import static org.mockito.Mockito.*;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.FilePath;
import io.jenkins.plugins.reporter.ReportAction;
import io.jenkins.plugins.reporter.ReportResult;
import io.jenkins.plugins.reporter.model.Report;


public class ReportRecorderTest {

    @Test
    public void testReadResolve_migratesOldConfiguration() throws Exception {
        ReportRecorder recorder = new ReportRecorder();

        // Define test values
        String oldName = "Old Test Report";
        String oldDisplayType = "absolute";
        Json oldProvider = new Json();
        // Assuming Json provider might have a pattern or other relevant field to set for a complete test.
        // If Json has a DataBoundConstructor or DataBoundSetters, they would be used by XStream.
        // For simplicity, we'll just use a new instance. If it requires parameters, this might need adjustment.
        // e.g. oldProvider.setPattern("*.json"); // if Json has such a setter

        // Use reflection to set the transient fields, simulating XStream's population
        // of these fields from an old configuration file.
        Field nameField = ReportRecorder.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(recorder, oldName);

        Field providerField = ReportRecorder.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(recorder, oldProvider);

        Field displayTypeField = ReportRecorder.class.getDeclaredField("displayType");
        displayTypeField.setAccessible(true);
        displayTypeField.set(recorder, oldDisplayType);

        // Call readResolve - this happens automatically during XStream deserialization,
        // but we call it directly for testing.
        Object resolvedObject = recorder.readResolve();
        assertTrue("readResolve should return an instance of ReportRecorder", resolvedObject instanceof ReportRecorder);
        ReportRecorder resolvedRecorder = (ReportRecorder) resolvedObject;

        // Assertions
        assertNotNull("ReportConfigs list should not be null after readResolve", resolvedRecorder.getReportConfigs());
        assertEquals("ReportConfigs list should contain exactly one migrated configuration", 1, resolvedRecorder.getReportConfigs().size());

        ReportRecorder.ReportConfig migratedConfig = resolvedRecorder.getReportConfigs().get(0);
        assertNotNull("Migrated config should not be null", migratedConfig);
        assertEquals("Migrated name does not match the old name", oldName, migratedConfig.getName());
        assertSame("Migrated provider instance does not match the old provider instance", oldProvider, migratedConfig.getProvider());
        assertEquals("Migrated displayType does not match the old displayType", oldDisplayType, migratedConfig.getDisplayType());

        // Assert that the old transient fields are now null
        // Accessing them via reflection again on the 'resolvedRecorder'
        assertNull("Old 'name' field should be null after migration", nameField.get(resolvedRecorder));
        assertNull("Old 'provider' field should be null after migration", providerField.get(resolvedRecorder));
        assertNull("Old 'displayType' field should be null after migration", displayTypeField.get(resolvedRecorder));
    }

    @Test
    public void testPerform_processesAllReportConfigs() throws IOException, InterruptedException {
        ReportRecorder recorder = new ReportRecorder();
        ReportRecorder spyRecorder = spy(recorder); // Use a spy to stub scan and publishReport

        // Mock Providers
        Provider mockProvider1 = mock(Provider.class);
        when(mockProvider1.getSymbolName()).thenReturn("provider1");
        Provider mockProvider2 = mock(Provider.class);
        when(mockProvider2.getSymbolName()).thenReturn("provider2");

        // Create ReportConfig instances (cannot mock ReportConfig directly if it's an inner class and new-ed up)
        ReportRecorder.ReportConfig config1 = new ReportRecorder.ReportConfig("Report1", mockProvider1, "absolute");
        ReportRecorder.ReportConfig config2 = new ReportRecorder.ReportConfig("Report2", mockProvider2, "pie");

        spyRecorder.setReportConfigs(List.of(config1, config2));

        // Mock Jenkins model objects
        Run<?, ?> mockRun = mock(Run.class);
        FilePath mockWorkspace = mock(FilePath.class);
        TaskListener mockListener = mock(TaskListener.class);
        PrintStream mockPrintStream = mock(PrintStream.class);
        when(mockListener.getLogger()).thenReturn(mockPrintStream);

        // Mock internal results of scan and publishReport
        Report mockReport1 = mock(Report.class);
        Report mockReport2 = mock(Report.class);
        ReportResult mockReportResult1 = mock(ReportResult.class);
        ReportResult mockReportResult2 = mock(ReportResult.class); // Different result for the second report

        // Stub the 'scan' method calls
        // Since scan is not private, it can be stubbed on a spy.
        // It creates 'new ReportScanner' internally, but we mock its output.
        doReturn(mockReport1).when(spyRecorder).scan(eq(mockRun), eq(mockWorkspace), eq(mockListener), eq(mockProvider1));
        doReturn(mockReport2).when(spyRecorder).scan(eq(mockRun), eq(mockWorkspace), eq(mockListener), eq(mockProvider2));

        // Stub the 'publishReport' method calls
        // It creates 'new ReportPublisher' internally, but we mock its output.
        doReturn(mockReportResult1).when(spyRecorder).publishReport(eq(mockRun), eq(mockListener), eq("provider1"), eq(mockReport1));
        doReturn(mockReportResult2).when(spyRecorder).publishReport(eq(mockRun), eq(mockListener), eq("provider2"), eq(mockReport2));

        // Call the method under test
        ReportResult finalResult = spyRecorder.perform(mockRun, mockWorkspace, mockListener);

        // Verify 'scan' was called for each provider
        verify(spyRecorder, times(1)).scan(eq(mockRun), eq(mockWorkspace), eq(mockListener), eq(mockProvider1));
        verify(spyRecorder, times(1)).scan(eq(mockRun), eq(mockWorkspace), eq(mockListener), eq(mockProvider2));

        // Verify 'setName' and 'setDisplayType' were called on the reports from scan
        verify(mockReport1, times(1)).setName("Report1");
        verify(mockReport1, times(1)).setDisplayType(io.jenkins.plugins.reporter.model.DisplayType.ABSOLUTE);
        verify(mockReport2, times(1)).setName("Report2");
        verify(mockReport2, times(1)).setDisplayType(io.jenkins.plugins.reporter.model.DisplayType.PIE);

        // Verify 'publishReport' was called for each report object
        verify(spyRecorder, times(1)).publishReport(eq(mockRun), eq(mockListener), eq("provider1"), eq(mockReport1));
        verify(spyRecorder, times(1)).publishReport(eq(mockRun), eq(mockListener), eq("provider2"), eq(mockReport2));

        // The perform method returns the result of the *last* processed report.
        assertSame("Final result should be the result of the last report published", mockReportResult2, finalResult);

        // Verify logging
        verify(mockListener.getLogger(), times(1)).println("[Reporter] Successfully processed report: Report1");
        verify(mockListener.getLogger(), times(1)).println("[Reporter] Successfully processed report: Report2");
    }
}
