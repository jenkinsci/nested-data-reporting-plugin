package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import io.jenkins.plugins.reporter.model.ReportParser;
import org.apache.poi.ss.usermodel.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Tests for the ExcelMulti provider class.
 * This test suite validates the functionality of the ExcelMulti provider,
 * which handles multi-sheet Excel files with header consistency enforcement.
 */
public class ExcelMultiTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    private File consistentHeadersFile;
    private File inconsistentHeadersFile;
    private File mixedValidityFile;
    private ExcelMulti provider;

    /**
     * Set up the test files and provider before each test.
     */
    @Before
    public void setUp() throws IOException {
        // Generate the test files if they don't exist
        String resourcesDir = "src/test/resources";
        consistentHeadersFile = new File(resourcesDir, "test-excel-multi-consistent.xlsx");
        inconsistentHeadersFile = new File(resourcesDir, "test-excel-multi-inconsistent.xlsx");
        mixedValidityFile = new File(resourcesDir, "test-excel-multi-mixed.xlsx");
        
        if (!consistentHeadersFile.exists()) {
            CreateExcelMultiSample.createConsistentHeadersExcelFile(consistentHeadersFile);
        }
        
        if (!inconsistentHeadersFile.exists()) {
            CreateExcelMultiSample.createInconsistentHeadersExcelFile(inconsistentHeadersFile);
        }
        
        if (!mixedValidityFile.exists()) {
            CreateExcelMultiSample.createMixedValidityExcelFile(mixedValidityFile);
        }
        
        // Create and configure the provider
        provider = new ExcelMulti();
        provider.setId("test-excel-multi");
        
        // Add debug logging in setUp
        if (consistentHeadersFile.exists()) {
            System.out.println("Test file exists: " + consistentHeadersFile.getAbsolutePath());
            System.out.println("File size: " + consistentHeadersFile.length() + " bytes");
        } else {
            System.out.println("Test file does not exist: " + consistentHeadersFile.getAbsolutePath());
        }

        if (mixedValidityFile.exists()) {
            System.out.println("Test file exists: " + mixedValidityFile.getAbsolutePath());
            System.out.println("File size: " + mixedValidityFile.length() + " bytes");
        } else {
            System.out.println("Test file does not exist: " + mixedValidityFile.getAbsolutePath());
        }
        
        // Debug Excel structure
        try (Workbook workbook = WorkbookFactory.create(consistentHeadersFile)) {
            System.out.println("=== Excel Structure Debug (consistentHeadersFile) ===");
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + ": " + sheet.getSheetName());
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    System.out.println("  Headers:");
                    for (Cell cell : headerRow) {
                        System.out.println("    - " + cell.getStringCellValue());
                    }
                }
            }
            System.out.println("=========================");
        } catch (Exception e) {
            System.out.println("Error reading Excel structure: " + e.getMessage());
        }
        
        try (Workbook workbook = WorkbookFactory.create(mixedValidityFile)) {
            System.out.println("=== Excel Structure Debug (mixedValidityFile) ===");
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                System.out.println("Sheet " + i + ": " + sheet.getSheetName());
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    System.out.println("  Headers:");
                    for (Cell cell : headerRow) {
                        System.out.println("    - " + cell.getStringCellValue());
                    }
                } else {
                    System.out.println("  No header row found");
                }
            }
            System.out.println("=========================");
        } catch (Exception e) {
            System.out.println("Error reading Excel structure: " + e.getMessage());
        }
    }

    /**
     * Test the basic functionality of the ExcelMulti provider.
     * This test verifies that the provider can parse a simple multi-sheet Excel file
     * with consistent headers across all sheets.
     */
    @Test
    public void testBasicFunctionality() throws IOException {
        // Create the parser
        ReportParser parser = provider.createParser();
        assertNotNull("Parser should not be null", parser);
        
        // Parse the file with consistent headers
        ReportDto report = parser.parse(consistentHeadersFile);
        
        // Verify the report is correctly parsed
        assertNotNull("Report should not be null", report);
        assertEquals("Report ID should match provider ID", "test-excel-multi", report.getId());
        
        // Check that we have items
        List<Item> items = report.getItems();
        assertNotNull("Items list should not be null", items);
        assertFalse("Items list should not be empty", items.isEmpty());
    }

    /**
     * Test that data from all sheets with consistent headers is aggregated.
     * This test verifies that the ExcelMulti provider correctly aggregates data
     * from all sheets that have consistent headers.
     */
    @Test
    public void testDataAggregation() throws IOException {
        // Create the parser
        ReportParser parser = provider.createParser();
        
        // Parse the file with consistent headers
        ReportDto report = parser.parse(consistentHeadersFile);
        
        // Verify data from all sheets is included
        List<Item> items = report.getItems();
        
        // Debug logging
        System.out.println("=== Data Aggregation Test Debug ===");
        System.out.println("Total items found: " + items.size());
        for (Item item : items) {
            System.out.println("Found category: " + item.getName());
            if (item.getItems() != null) {
                for (Item subItem : item.getItems()) {
                    System.out.println("  - Subcategory: " + subItem.getName());
                }
            }
        }
        System.out.println("================================");
        
        // Find items from different sheets
        Optional<Item> categoryA = items.stream()
                .filter(item -> item.getName().equals("Category A"))
                .findFirst();
        
        Optional<Item> categoryB = items.stream()
                .filter(item -> item.getName().equals("Category B"))
                .findFirst();
        
        Optional<Item> categoryC = items.stream()
                .filter(item -> item.getName().equals("Category C"))
                .findFirst();
        
        // Verify all categories are present (from different sheets)
        assertTrue("Category A should be present", categoryA.isPresent());
        assertTrue("Category B should be present", categoryB.isPresent());
        assertTrue("Category C should be present", categoryC.isPresent());
        
        // Verify subcategories for Category A
        if (categoryA.isPresent()) {
            List<Item> subcategoriesA = categoryA.get().getItems();
            assertNotNull("Subcategories for Category A should not be null", subcategoriesA);
            assertEquals("Category A should have 2 subcategories", 2, subcategoriesA.size());
            
            // Check subcategory names
            assertEquals("First subcategory of A should be Subcat A1", 
                    "Subcat A1", subcategoriesA.get(0).getName());
            assertEquals("Second subcategory of A should be Subcat A2", 
                    "Subcat A2", subcategoriesA.get(1).getName());
        }
        
        // Verify subcategories for Category B
        if (categoryB.isPresent()) {
            List<Item> subcategoriesB = categoryB.get().getItems();
            assertNotNull("Subcategories for Category B should not be null", subcategoriesB);
            assertEquals("Category B should have 2 subcategories", 2, subcategoriesB.size());
            
            // Check subcategory names
            assertEquals("First subcategory of B should be Subcat B1", 
                    "Subcat B1", subcategoriesB.get(0).getName());
            assertEquals("Second subcategory of B should be Subcat B2", 
                    "Subcat B2", subcategoriesB.get(1).getName());
        }
    }

    /**
     * Test that the ExcelMulti provider correctly enforces header consistency.
     * This test verifies that only sheets with consistent headers are processed.
     */
    @Test
    public void testHeaderConsistencyValidation() throws IOException {
        // Create the parser and make it an instance of ExcelMultiParser to access parser messages
        ExcelMulti.ExcelMultiParser parser = (ExcelMulti.ExcelMultiParser) provider.createParser();
        
        // Parse the file with inconsistent headers
        ReportDto report = parser.parse(inconsistentHeadersFile);
        
        // Verify only data from the first sheet is included (as other sheets have inconsistent headers)
        List<Item> items = report.getItems();
        
        // There should only be Category A (from first sheet)
        Optional<Item> categoryA = items.stream()
                .filter(item -> item.getName().equals("Category A"))
                .findFirst();
        
        Optional<Item> categoryB = items.stream()
                .filter(item -> item.getName().equals("Category B"))
                .findFirst();
        
        Optional<Item> categoryC = items.stream()
                .filter(item -> item.getName().equals("Category C"))
                .findFirst();
        
        // Verify only Category A is present (as other sheets have inconsistent headers)
        assertTrue("Category A should be present", categoryA.isPresent());
        assertFalse("Category B should not be present", categoryB.isPresent());
        assertFalse("Category C should not be present", categoryC.isPresent());
    }

    /**
     * Test that the ExcelMulti provider correctly handles invalid sheets.
     * This test verifies that invalid sheets are skipped and valid sheets are processed.
     */
    @Test
    public void testHandlingInvalidSheets() throws IOException {
        // Create the parser
        ReportParser parser = provider.createParser();
        
        // Parse the file with mixed validity
        ReportDto report = parser.parse(mixedValidityFile);
        
        // Verify only data from valid sheets is included
        List<Item> items = report.getItems();
        
        // Debug logging
        System.out.println("=== Invalid Sheets Test Debug ===");
        System.out.println("Total items found: " + items.size());
        for (Item item : items) {
            System.out.println("Found category: " + item.getName());
            if (item.getItems() != null) {
                for (Item subItem : item.getItems()) {
                    System.out.println("  - Subcategory: " + subItem.getName());
                }
            }
        }
        System.out.println("================================");
        
        // There should be Categories A and C (from valid sheets)
        Optional<Item> categoryA = items.stream()
                .filter(item -> item.getName().equals("Category A"))
                .findFirst();
        
        Optional<Item> categoryC = items.stream()
                .filter(item -> item.getName().equals("Category C"))
                .findFirst();
        
        Optional<Item> categoryD = items.stream()
                .filter(item -> item.getName().equals("Category D"))
                .findFirst();
        
        // Verify only Categories A and C are present (from valid sheets)
        assertTrue("Category A should be present", categoryA.isPresent());
        assertTrue("Category C should be present", categoryC.isPresent());
        assertFalse("Category D should not be present", categoryD.isPresent());
    }

    /**
     * Test that the ExcelMulti provider generates appropriate parser messages.
     * This test verifies that detailed messages are generated for skipped sheets.
     */
    @Test
    public void testParserMessages() throws IOException {
        // Create the parser and make it an instance of ExcelMultiParser to access parser messages
        ExcelMulti.ExcelMultiParser parser = (ExcelMulti.ExcelMultiParser) provider.createParser();
        
        // Parse the file with mixed validity
        parser.parse(mixedValidityFile);
        
        // Get parser messages
        List<String> messages = parser.parserMessages;
        
        // Verify that parser messages were generated
        assertFalse("Parser messages should not be empty", messages.isEmpty());
        
        // Check for expected messages
        boolean foundInitialMessage = false;
        boolean foundSkippedEmptySheet = false;
        boolean foundSkippedFewerColumnsSheet = false;
        boolean foundSummaryMessage = false;
        
        for (String message : messages) {
            if (message.contains("Excel file contains")) {
                foundInitialMessage = true;
            }
            if (message.contains("Empty Sheet")) {
                foundSkippedEmptySheet = true;
            }
            if (message.contains("Invalid Sheet - Fewer Columns")) {
                foundSkippedFewerColumnsSheet = true;
            }
            if (message.contains("Skipped") && message.contains("sheets:")) {
                foundSummaryMessage = true;
            }
        }
        
        assertTrue("Should have initial message about sheet count", foundInitialMessage);
        assertTrue("Should have message about skipped empty sheet", foundSkippedEmptySheet);
        assertTrue("Should have message about skipped sheet with fewer columns", foundSkippedFewerColumnsSheet);
        assertTrue("Should have summary message about skipped sheets", foundSummaryMessage);
    }

    /**
     * Test that the ExcelMulti provider correctly handles error cases.
     * This test verifies that appropriate exceptions are thrown for invalid files.
     */
    @Test(expected = IOException.class)
    public void testErrorHandling() throws IOException {
        // Create the parser
        ReportParser parser = provider.createParser();
        
        // Try to parse a non-existent file
        File nonExistentFile = new File("non-existent-file.xlsx");
        parser.parse(nonExistentFile);
        
        // Should throw IOException
    }
}

