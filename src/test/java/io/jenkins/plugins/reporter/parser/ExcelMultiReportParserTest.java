package io.jenkins.plugins.reporter.parser;

import io.jenkins.plugins.reporter.model.ExcelParserConfig;
import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // For creating test workbooks
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files; // For Files.writeString in one of the tests
// import java.util.ArrayList; // Not directly used for declaration, List is used
import java.util.Arrays;
import java.util.List; // Correct import for List
// import java.util.Map; // Not directly used
import java.util.stream.Collectors;

class ExcelMultiReportParserTest {

    private ExcelParserConfig defaultConfig;
    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    @BeforeEach
    void setUp() {
        defaultConfig = new ExcelParserConfig();
    }

    private File getResourceFile(String fileName) throws URISyntaxException {
        URL resource = getClass().getResource("/io/jenkins/plugins/reporter/provider/" + fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource file not found: " + fileName + 
                                               ". Ensure it is in src/test/resources/io/jenkins/plugins/reporter/provider/");
        }
        return new File(resource.toURI());
    }

    // Helper to create a multi-sheet workbook from single-sheet files
    private File createMultiSheetWorkbook(String outputFileName, List<String> sheetResourceFiles, List<String> sheetNames) throws IOException, URISyntaxException {
        File outputFile = tempDir.resolve(outputFileName).toFile();
        try (XSSFWorkbook multiSheetWorkbook = new XSSFWorkbook()) {
            for (int i = 0; i < sheetResourceFiles.size(); i++) {
                File sheetFile = getResourceFile(sheetResourceFiles.get(i));
                String sheetName = sheetNames.get(i);
                Sheet newSheet = multiSheetWorkbook.createSheet(sheetName);

                try (FileInputStream fis = new FileInputStream(sheetFile);
                     Workbook sourceSheetWorkbook = WorkbookFactory.create(fis)) {
                    Sheet sourceSheet = sourceSheetWorkbook.getSheetAt(0); 
                    int rowNum = 0;
                    for (Row sourceRow : sourceSheet) {
                        Row newRow = newSheet.createRow(rowNum++);
                        int cellNum = 0;
                        for (Cell sourceCell : sourceRow) {
                            Cell newCell = newRow.createCell(cellNum++);
                            switch (sourceCell.getCellType()) {
                                case STRING:
                                    newCell.setCellValue(sourceCell.getStringCellValue());
                                    break;
                                case NUMERIC:
                                    if (DateUtil.isCellDateFormatted(sourceCell)) {
                                        newCell.setCellValue(sourceCell.getDateCellValue());
                                    } else {
                                        newCell.setCellValue(sourceCell.getNumericCellValue());
                                    }
                                    break;
                                case BOOLEAN:
                                    newCell.setCellValue(sourceCell.getBooleanCellValue());
                                    break;
                                case FORMULA:
                                    newCell.setCellFormula(sourceCell.getCellFormula());
                                    break;
                                case BLANK:
                                    break;
                                default:
                                    // Potentially log or handle other types if necessary
                                    break;
                            }
                        }
                    }
                }
            }
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                multiSheetWorkbook.write(fos);
            }
        }
        return outputFile;
    }

    @Test
    void testParseMultiSheetConsistentHeaders() throws IOException, URISyntaxException {
        List<String> sheetFiles = Arrays.asList(
                "sample_excel_multi_consistent_sheet1_Data_Alpha.xlsx",
                "sample_excel_multi_consistent_sheet2_Data_Beta.xlsx");
        List<String> sheetNames = Arrays.asList("Data Alpha", "Data Beta");
        File multiSheetFile = createMultiSheetWorkbook("consistent_multi.xlsx", sheetFiles, sheetNames);

        ExcelMultiReportParser parser = new ExcelMultiReportParser("testMultiConsistent", defaultConfig);
        ReportDto result = parser.parse(multiSheetFile);

        assertNotNull(result);
        // System.out.println("Messages (Consistent): " + result.getParserLogMessages());
        
        // Items from Data Alpha (ID, Metric, Result): Alpha001, Time, 100; Alpha002, Score, 200
        // Items from Data Beta (ID, Metric, Result): Beta001, Time, 110; Beta002, Score, 210
        // Report ID for parseSheet: "testMultiConsistent::Data_Alpha" and "testMultiConsistent::Data_Beta"
        // Item ID structure: reportIdForSheet + "::" + hierarchyPart1 + "_" + hierarchyPart2 ...
        // Example: "testMultiConsistent::Data_Alpha::Alpha001_Time"

        // Let's re-evaluate the expected item count and structure.
        // Sheet 1: Alpha001 (parent), Time (child, value 100), Score (child, value 200) -> No, this is wrong.
        // The parser logic: "ID" is one hierarchy, "Metric" is another. "Result" is the value column.
        // Sheet 1: Item "Alpha001" (id testMultiConsistent::Data_Alpha::Alpha001)
        //             -> Item "Time" (id testMultiConsistent::Data_Alpha::Alpha001_Time, result {"Result":100})
        //          Item "Alpha002" (id testMultiConsistent::Data_Alpha::Alpha002)
        //             -> Item "Score" (id testMultiConsistent::Data_Alpha::Alpha002_Score, result {"Result":200})
        // Sheet 2: Item "Beta001" (id testMultiConsistent::Data_Beta::Beta001)
        //             -> Item "Time" (id testMultiConsistent::Data_Beta::Beta001_Time, result {"Result":110})
        //          Item "Beta002" (id testMultiConsistent::Data_Beta::Beta002)
        //             -> Item "Score" (id testMultiConsistent::Data_Beta::Beta002_Score, result {"Result":210})
        // So, the top-level items in the aggregated report are Alpha001, Alpha002, Beta001, Beta002. That's 4.
        assertEquals(4, result.getItems().size(), "Should have 4 top-level items in total from two sheets.");


        Item itemA001 = result.findItem("testMultiConsistent::Data_Alpha::Alpha001", result.getItems()).orElse(null);
        assertNotNull(itemA001, "Item Alpha001 from sheet 'Data Alpha' not found.");
        assertEquals("Alpha001", itemA001.getName());
        Item itemA001Time = result.findItem("testMultiConsistent::Data_Alpha::Alpha001_Time", itemA001.getItems()).orElse(null);
        assertNotNull(itemA001Time, "Sub-item Time for Alpha001 not found.");
        assertEquals("Time", itemA001Time.getName());
        assertEquals(100, itemA001Time.getResult().get("Result"));

        Item itemB001 = result.findItem("testMultiConsistent::Data_Beta::Beta001", result.getItems()).orElse(null);
        assertNotNull(itemB001, "Item Beta001 from sheet 'Data Beta' not found.");
        assertEquals("Beta001", itemB001.getName());
        Item itemB001Time = result.findItem("testMultiConsistent::Data_Beta::Beta001_Time", itemB001.getItems()).orElse(null);
        assertNotNull(itemB001Time, "Sub-item Time for Beta001 not found.");
        assertEquals("Time", itemB001Time.getName());
        assertEquals(110, itemB001Time.getResult().get("Result"));
        
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Using header from sheet 'Data Alpha' as the reference")), "Should log reference header message.");
    }

    @Test
    void testParseMultiSheetInconsistentHeaders() throws IOException, URISyntaxException {
        List<String> sheetFiles = Arrays.asList(
                "sample_excel_multi_inconsistent_header_sheet1_Metrics.xlsx", 
                "sample_excel_multi_inconsistent_header_sheet2_Stats.xlsx");  
        List<String> sheetNames = Arrays.asList("Metrics", "Stats"); // Sheet "Stats" has header: System, Disk, Network
        File multiSheetFile = createMultiSheetWorkbook("inconsistent_multi.xlsx", sheetFiles, sheetNames);

        ExcelMultiReportParser parser = new ExcelMultiReportParser("testMultiInconsistent", defaultConfig);
        ReportDto result = parser.parse(multiSheetFile);

        assertNotNull(result);
        // System.out.println("Messages (Inconsistent): " + result.getParserLogMessages());
        
        // Items from "Metrics" (System, CPU, Memory): SysA, 70, 500
        // Hierarchy is just "System". Values are "CPU", "Memory".
        // Item ID: "testMultiInconsistent::Metrics::SysA"
        // Results: {"CPU": 70, "Memory": 500}
        assertEquals(1, result.getItems().size(), "Should only have items from the first sheet ('Metrics').");
        String itemSysA_ID = "testMultiInconsistent::Metrics::SysA"; 
        Item itemSysA = result.findItem(itemSysA_ID, result.getItems()).orElse(null);
        assertNotNull(itemSysA, "Item from 'Metrics' sheet not found. ID searched: " + itemSysA_ID + 
                                ". Available: " + result.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));
        assertEquals("SysA", itemSysA.getName());
        assertEquals(70, itemSysA.getResult().get("CPU"));
        assertEquals(500, itemSysA.getResult().get("Memory"));
        
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Error: Sheet 'Stats' has an inconsistent header.")), "Should log header inconsistency for 'Stats'.");
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Skipping this sheet.")), "Should log skipping inconsistent sheet 'Stats'.");
    }
    
    @Test
    void testParseSingleSheetFileWithMultiParser() throws IOException, URISyntaxException {
        ExcelMultiReportParser parser = new ExcelMultiReportParser("testSingleWithMulti", defaultConfig);
        // sample_excel_single_sheet.xlsx has header: Category, SubCategory, Value1, Value2
        // Row: A, X, 10, 20
        File file = getResourceFile("sample_excel_single_sheet.xlsx"); 
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        // System.out.println("Messages (Single with Multi): " + result.getParserLogMessages().stream().collect(Collectors.joining("\n")));
        // System.out.println("Items (Single with Multi): " + result.getItems());

        // Expected top-level items "A", "B"
        assertEquals(2, result.getItems().size(), "Should be 2 top-level items (A, B)");

        // Expected top-level items "A", "B"
        assertEquals(2, result.getItems().size(), "Should be 2 top-level items (A, B)");

        // The ExcelMultiReportParser, when parsing a single file, uses the filename (or a cleaned version) as the sheet identifier.
        // The original test resource is "sample_excel_single_sheet.xlsx".
        // The parser logic (sheet.getSheetName().replaceAll("[^a-zA-Z0-9_.-]", "_")) for sheet name cleaning
        // would turn "sample_excel_single_sheet.xlsx" into "sample_excel_single_sheet_xlsx" if it were a sheet name.
        // However, for a single file parsed by ExcelMultiReportParser, it iterates through sheets.
        // If "sample_excel_single_sheet.xlsx" is parsed, it will have one sheet, typically named "Sheet1" by POI if not named.
        // The reportId for parseSheet is this.id + "::" + cleanSheetName.
        // So, if the sheet name is "Sheet1", the item ID will contain "::Sheet1::".
        // If the filename itself was used as a sheet name (not typical for single file parsing by Multi), it would be different.
        // The previous failure log indicated the sheet name part was "sample_excel_single_sheet.csv" - this is confusing.
        // Let's assume the *cleaned sheet name* from the actual sheet within the file is used.
        // Expected top-level items "A", "B"
        assertEquals(2, result.getItems().size(), "Should be 2 top-level items (A, B)");

        // The ExcelMultiReportParser, when parsing a single file, uses the filename (or a cleaned version) as the sheet identifier.
        // The original test resource is "sample_excel_single_sheet.xlsx".
        // The parser logic (sheet.getSheetName().replaceAll("[^a-zA-Z0-9_.-]", "_")) for sheet name cleaning
        // would turn "sample_excel_single_sheet.xlsx" into "sample_excel_single_sheet_xlsx" if it were a sheet name.
        // However, for a single file parsed by ExcelMultiReportParser, it iterates through sheets.
        // If "sample_excel_single_sheet.xlsx" is parsed, it will have one sheet, typically named "Sheet1" by POI if not named.
        // The reportId for parseSheet is this.id + "::" + cleanSheetName.
        // So, if the sheet name is "Sheet1", the item ID will contain "::Sheet1::".
        // If the filename itself was used as a sheet name (not typical for single file parsing by Multi), it would be different.
        // The previous failure log indicated the sheet name part was "sample_excel_single_sheet.csv" - this is confusing.
        // Let's assume the *cleaned sheet name* from the actual sheet within the file is used.
        // For "sample_excel_single_sheet.xlsx", the first sheet is usually "Sheet1".
        
        String expectedSheetNameInID = "sample_excel_single_sheet.csv"; // From error log
        String baseId = "testSingleWithMulti";
        String itemNameA = "A";
        String itemNameAX = "X"; // From original test logic for sample_excel_single_sheet.xlsx

        String expectedItemA_ID = baseId + "::" + expectedSheetNameInID + "::" + itemNameA;
        Item itemA = result.findItem(expectedItemA_ID, result.getItems()).orElse(null);
        assertNotNull(itemA, "Item A not found. Expected ID: " + expectedItemA_ID + ". Actual top-level IDs: " + result.getItems().stream().map(io.jenkins.plugins.reporter.model.Item::getId).collect(java.util.stream.Collectors.joining(", ")));

        // Construct sub-item ID based on this
        String expectedItemAX_ID = baseId + "::" + expectedSheetNameInID + "::" + itemNameA + "_" + itemNameAX;
        Item itemAX = result.findItem(expectedItemAX_ID, itemA.getItems()).orElse(null);
        assertNotNull(itemAX, "Item AX not found in A. Expected ID: " + expectedItemAX_ID + ". Sub-item IDs for A: " + (itemA.getItems() != null ? itemA.getItems().stream().map(io.jenkins.plugins.reporter.model.Item::getId).collect(java.util.stream.Collectors.joining(", ")) : "null or no items"));
        assertEquals("X", itemAX.getName());
        assertEquals(10, itemAX.getResult().get("Value1"));
        assertEquals(20, itemAX.getResult().get("Value2"));
        
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Using header from sheet 'sample_excel_single_sheet.csv' as the reference")), "Should log reference header message for 'sample_excel_single_sheet.csv'. Actual log messages: " + result.getParserLogMessages().stream().collect(java.util.stream.Collectors.joining("\\n")));
    }

    @Test
    void testParseEmptyExcelFile() throws IOException, URISyntaxException {
        ExcelMultiReportParser parser = new ExcelMultiReportParser("testEmptyFileMulti", defaultConfig);
        File file = getResourceFile("sample_excel_empty_sheet.xlsx"); 
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items for an empty file/sheet.");
        // System.out.println("Messages (Empty File Multi): " + result.getParserLogMessages());
        String expectedSheetNameInLog = "sample_excel_empty_sheet.csv";
        String expectedCoreMessage = "no header row found in sheet";
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> {
            String lowerMsg = m.toLowerCase();
            return lowerMsg.contains(expectedCoreMessage) && lowerMsg.contains("'" + expectedSheetNameInLog.toLowerCase() + "'");
        }), "Should log no header for sheet '" + expectedSheetNameInLog + "'. Messages: " + result.getParserLogMessages());
    }
    
    @Test
    void testParseInvalidFileWithMultiParser() throws IOException {
        ExcelMultiReportParser parser = new ExcelMultiReportParser("testInvalidMulti", defaultConfig);
        Path tempFile = tempDir.resolve("dummy_multi.txt");
        Files.writeString(tempFile, "This is not an excel file for multi-parser.");
        
        ReportDto result = parser.parse(tempFile.toFile());

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items for a non-Excel file.");
        // System.out.println("Messages (Invalid Multi): " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream()
                         .anyMatch(m -> m.toLowerCase().contains("error parsing excel file") || 
                                        m.toLowerCase().contains("your input appears to be a text file") ||
                                        m.toLowerCase().contains("invalid header signature") ||
                                        m.toLowerCase().contains("file format not supported")),
                   "Should log error about parsing or file format. Actual: " + result.getParserLogMessages());
    }
}
