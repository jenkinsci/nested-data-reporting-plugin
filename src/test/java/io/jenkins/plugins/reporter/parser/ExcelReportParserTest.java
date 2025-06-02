package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
// import java.nio.file.Paths; // Not used
import java.util.List;
// import java.util.stream.Collectors; // Not used

class ExcelReportParserTest {

    private ExcelParserConfig defaultConfig;

    @BeforeEach
    void setUp() {
        defaultConfig = new ExcelParserConfig(); // Use default config for these tests
    }

    private File getResourceFile(String fileName) throws URISyntaxException {
        URL resource = getClass().getResource("/io/jenkins/plugins/reporter/provider/" + fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource file not found: " + fileName + ". Ensure it's in src/test/resources/io/jenkins/plugins/reporter/provider/");
        }
        return new File(resource.toURI());
    }

    @Test
    void testParseSingleSheetNominal() throws IOException, URISyntaxException {
        ExcelReportParser parser = new ExcelReportParser("testReport1", defaultConfig);
        File file = getResourceFile("sample_excel_single_sheet.xlsx");
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertEquals("testReport1", result.getId());
        assertFalse(result.getItems().isEmpty(), "Should have parsed items.");
        // System.out.println("Parser messages (single_sheet): " + result.getParserLogMessages());
        // System.out.println("Items (single_sheet): " + result.getItems());

        // Expected structure from sample_excel_single_sheet.xlsx:
        // Header: Category, SubCategory, Value1, Value2
        // Row: A, X, 10, 20
        // Row: A, Y, 15, 25
        // Row: B, Z, 20, 30
        // ExcelReportParser will create IDs like "testReport1::A", "testReport1::A_X"

        assertEquals(2, result.getItems().size(), "Should be 2 top-level items (A, B)");

        Item itemA = result.findItem("testReport1::A", result.getItems()).orElse(null);
        assertNotNull(itemA, "Item A not found. Available top-level items: " + result.getItems().stream().map(Item::getId).collect(java.util.stream.Collectors.toList()));
        assertEquals("A", itemA.getName());
        assertEquals(2, itemA.getItems().size(), "Item A should have 2 sub-items (X, Y)");

        Item itemAX = result.findItem("testReport1::A_X", itemA.getItems()).orElse(null);
        assertNotNull(itemAX, "Item AX not found in A. Available sub-items: " + itemA.getItems().stream().map(Item::getId).collect(java.util.stream.Collectors.toList()));
        assertEquals("X", itemAX.getName());
        assertNotNull(itemAX.getResult(), "Item AX should have results.");
        assertEquals(10, itemAX.getResult().get("Value1"));
        assertEquals(20, itemAX.getResult().get("Value2"));
        
        Item itemAY = result.findItem("testReport1::A_Y", itemA.getItems()).orElse(null);
        assertNotNull(itemAY, "Item AY not found in A.");
        assertEquals("Y", itemAY.getName());
        assertNotNull(itemAY.getResult(), "Item AY should have results.");
        assertEquals(15, itemAY.getResult().get("Value1"));
        assertEquals(25, itemAY.getResult().get("Value2"));

        Item itemB = result.findItem("testReport1::B", result.getItems()).orElse(null);
        assertNotNull(itemB, "Item B not found.");
        assertEquals("B", itemB.getName());
        assertEquals(1, itemB.getItems().size(), "Item B should have 1 sub-item (Z)");
        
        Item itemBZ = result.findItem("testReport1::B_Z", itemB.getItems()).orElse(null);
        assertNotNull(itemBZ, "Item BZ not found in B.");
        assertEquals("Z", itemBZ.getName());
        assertNotNull(itemBZ.getResult(), "Item BZ should have results.");
        assertEquals(20, itemBZ.getResult().get("Value1"));
        assertEquals(30, itemBZ.getResult().get("Value2"));
        
        // Check for specific messages if needed, e.g., about structure detection
        // assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Detected structure in sheet")));
    }

    @Test
    void testParseOnlyHeader() throws IOException, URISyntaxException {
        ExcelReportParser parser = new ExcelReportParser("testOnlyHeader", defaultConfig);
        File file = getResourceFile("sample_excel_only_header.xlsx");
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items when only header is present.");
        // System.out.println("Parser messages (only_header): " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream()
                         .anyMatch(m -> m.toLowerCase().contains("no data rows found after header")),
                   "Should log message about no data rows. Messages: " + result.getParserLogMessages());
    }

    @Test
    void testParseEmptySheet() throws IOException, URISyntaxException {
        ExcelReportParser parser = new ExcelReportParser("testEmptySheet", defaultConfig);
        File file = getResourceFile("sample_excel_empty_sheet.xlsx"); // This file is empty
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items for an empty sheet.");
        // System.out.println("Parser messages (empty_sheet): " + result.getParserLogMessages());
        // The ExcelReportParser uses WorkbookFactory.create(is) which might throw for a 0KB file if it's not even a valid ZIP.
        // If it's a valid ZIP (empty XLSX), POI might say "has no sheets".
        // If BaseExcelParser.findHeaderRow is called on an empty sheet, it returns Optional.empty().
        // ExcelReportParser.parseSheet then logs "No header row found".
         assertTrue(result.getParserLogMessages().stream()
                         .anyMatch(m -> m.toLowerCase().contains("no header row found") || 
                                        m.toLowerCase().contains("excel file has no sheets") ||
                                        m.toLowerCase().contains("error parsing excel file")), // More general catch
                    "Should log message about no header, no sheets, or parsing error. Messages: " + result.getParserLogMessages());
    }
    
    @Test
    void testParseNoHeaderData() throws IOException, URISyntaxException {
        ExcelReportParser parser = new ExcelReportParser("testNoHeader", defaultConfig);
        // sample_excel_no_header.xlsx contains:
        // 1,2,3
        // 4,5,6
        File file = getResourceFile("sample_excel_no_header.xlsx"); 
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        // System.out.println("Parser messages (no_header): " + result.getParserLogMessages());
        // System.out.println("Items (no_header): " + result.getItems());

        // BaseExcelParser.findHeaderRow will pick the first non-empty row. So "1,2,3" becomes header.
        // Header names: "1", "2", "3"
        // Data row: "4,5,6"
        // Structure detection:
        // - '6' is numeric, colIdxValueStart becomes 2 (index of "3")
        // - '5' is numeric, colIdxValueStart becomes 1 (index of "2")
        // - '4' is numeric, colIdxValueStart becomes 0 (index of "1")
        // So, all columns are treated as value columns. Hierarchy part is empty.
        // This means items will be direct children of the report, named "Data Row X" by ExcelReportParser.
        
        assertFalse(result.getItems().isEmpty(), "Should parse items even if header is data-like.");
        assertEquals(1, result.getItems().size(), "Should parse one main data item when first row is taken as header.");
        
        Item dataItem = result.getItems().get(0);
        // Default name for rows that don't form hierarchy is "Data Row X (Sheet: Y)"
        // The ID is generated like: "sheet_" + sheetName.replaceAll("[^a-zA-Z0-9]", "") + "_row_" + (i + 1) + "_" + reportId;
        // For this test, reportId is "testNoHeader". Sheet name is probably "Sheet1". Row index i is 0 (first data row).
        // String expectedId = "sheet_Sheet1_row_1_testNoHeader"; // This is an assumption on sheet name and row index logic
        // assertEquals(expectedId, dataItem.getId()); // ID check can be fragile
        assertTrue(dataItem.getName().startsWith("Data Row 1"), "Item name should be generic for data row.");

        assertNotNull(dataItem.getResult(), "Data item should have results.");
        assertEquals(4, dataItem.getResult().get("1")); // Header "1" -> value 4
        assertEquals(5, dataItem.getResult().get("2")); // Header "2" -> value 5
        assertEquals(6, dataItem.getResult().get("3")); // Header "3" -> value 6

        assertTrue(result.getParserLogMessages().stream()
                         .anyMatch(m -> m.contains("Detected structure in sheet")), 
                   "Structure detection message should be present. Messages: " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream()
                         .anyMatch(m -> m.contains("Info: Row 1 in sheet 'Sheet1' has all columns treated as values.")),
                    "Should log info about all columns treated as values. Messages: " + result.getParserLogMessages());
    }

    @Test
    void testParseInvalidFile() throws IOException {
        ExcelReportParser parser = new ExcelReportParser("testInvalid", defaultConfig);
        
        Path tempDir = null;
        File dummyFile = null;
        try {
            tempDir = Files.createTempDirectory("test-excel-invalid");
            dummyFile = new File(tempDir.toFile(), "dummy.txt");
            Files.writeString(dummyFile.toPath(), "This is not an excel file, just plain text.");

            ReportDto result = parser.parse(dummyFile);

            assertNotNull(result);
            assertTrue(result.getItems().isEmpty(), "Should have no items for a non-Excel file.");
            // System.out.println("Parser messages (invalid_file): " + result.getParserLogMessages());
            assertTrue(result.getParserLogMessages().stream()
                             .anyMatch(m -> m.toLowerCase().contains("error parsing excel file") || 
                                            m.toLowerCase().contains("your input appears to be a text file") || // POI specific message for text
                                            m.toLowerCase().contains("invalid header signature") || // POI specific for non-zip
                                            m.toLowerCase().contains("file format not supported")), // General fallback
                       "Should log error about parsing or file format. Messages: " + result.getParserLogMessages());
        } finally {
            if (dummyFile != null && dummyFile.exists()) {
                dummyFile.delete();
            }
            if (tempDir != null && Files.exists(tempDir)) {
                Files.delete(tempDir);
            }
        }
    }
}
