package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.Item;
import io.jenkins.plugins.reporter.model.ReportDto;
import org.junit.jupiter.api.Test; // Combined BeforeEach and Test from correct package
import org.junit.jupiter.api.BeforeEach; // Explicitly for clarity, though Test covers it

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
// import java.nio.file.Paths; // Not currently used
// import java.util.List; // Used via specific classes like ArrayList or via stream().collect()
// import java.util.Map; // Used via item.getResult()
import java.util.stream.Collectors;


class CsvCustomParserTest {

    // Csv.CsvCustomParser is a public static inner class, so we can instantiate it directly.
    // private Csv csvProvider; // Not strictly needed if CsvCustomParser is static and public

    @BeforeEach
    void setUp() {
        // No setup needed here if we directly instantiate CsvCustomParser
    }

    private File getResourceFile(String fileName) throws URISyntaxException {
        URL resource = getClass().getResource("/io/jenkins/plugins/reporter/provider/" + fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Test resource file not found: " + fileName + 
                                               ". Ensure it is in src/test/resources/io/jenkins/plugins/reporter/provider/");
        }
        return new File(resource.toURI());
    }
    
    @Test
    void testParseStandardCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("standard");
        File file = getResourceFile("sample_csv_standard.csv"); // Host,CPU,RAM,Disk -> server1,75,16,500
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertEquals("standard", result.getId());
        assertFalse(result.getItems().isEmpty(), "Should parse items.");
        // System.out.println("Messages (Standard CSV): " + result.getParserLogMessages());
        // System.out.println("Items (Standard CSV): " + result.getItems());

        assertEquals(2, result.getItems().size());
        Item server1 = result.findItem("server1", result.getItems()).orElse(null); 
        assertNotNull(server1, "Item 'server1' not found. Found: " + result.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));
        assertEquals("server1", server1.getName());
        assertEquals(75, server1.getResult().get("CPU"));
        assertEquals(16, server1.getResult().get("RAM"));
        assertEquals(500, server1.getResult().get("Disk"));
        
        Item server2 = result.findItem("server2", result.getItems()).orElse(null); 
        assertNotNull(server2, "Item 'server2' not found.");
        assertEquals("server2", server2.getName());
        assertEquals(60, server2.getResult().get("CPU"));
        assertEquals(32, server2.getResult().get("RAM"));
        assertEquals(1000, server2.getResult().get("Disk"));
    }

    @Test
    void testParseSemicolonCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("semicolon");
        File file = getResourceFile("sample_csv_semicolon.csv"); // Product;Version;Count -> AppA;1.0;150
        ReportDto result = parser.parse(file);
        
        assertNotNull(result);
        // System.out.println("Messages (Semicolon CSV): " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Detected delimiter: ';'")), "Should log detected delimiter ';'");
        assertEquals(2, result.getItems().size()); // AppA, AppB
        
        // Hierarchy: Product -> Version. Value: Count
        Item appA = result.findItem("AppA", result.getItems()).orElse(null);
        assertNotNull(appA, "Item 'AppA' not found. Found: " + result.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));
        Item appAV1 = result.findItem("AppA1.0", appA.getItems()).orElse(null); // ID is "AppA" + "1.0"
        assertNotNull(appAV1, "Item 'AppA1.0' not found in AppA. Found: " + appA.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));
        assertEquals("1.0", appAV1.getName());
        assertEquals(150, appAV1.getResult().get("Count"));
    }
    
    @Test
    void testParseTabCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("tab");
        File file = getResourceFile("sample_csv_tab.csv"); // Name	Age	City -> John	30	New York
        ReportDto result = parser.parse(file);
        
        assertNotNull(result);
        // System.out.println("Messages (Tab CSV): " + result.getParserLogMessages());
        // System.out.println("Items (Tab CSV): " + result.getItems());
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Detected delimiter: '\t'")), "Should log detected delimiter '\\t'");
        assertEquals(2, result.getItems().size()); // John, Jane
        
        // Hierarchy: Name. Values: Age, City
        Item john = result.findItem("tab::John", result.getItems()).orElse(null);
        assertNotNull(john, "Item 'John' not found. Found: " + result.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));
        assertEquals("John", john.getName());
        assertEquals(30, john.getResult().get("Age"));
        assertEquals(0, john.getResult().get("City"), "Non-numeric 'City' in value part should result in 0, as per current CsvCustomParser int conversion.");
    }

    @Test
    void testParseLeadingEmptyLinesCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("leadingEmpty");
        File file = getResourceFile("sample_csv_leading_empty_lines.csv"); // (Potentially empty lines) ID,Name,Value -> 1,Test,100
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        // System.out.println("Messages (Leading Empty): " + result.getParserLogMessages());
        // System.out.println("Items (Leading Empty): " + result.getItems());

        // Refactored CsvParser: "ID" (1) is numeric -> colIdxValueStart=0. All values. Generic item names.
        // Header: ID, Name, Value. Data: 1, Test, 100.
        // Expect one generic item because the hierarchy part is empty.
        assertEquals(2, result.getItems().size(), "Should have 2 generic items, one for each data row.");

        Item item1 = result.getItems().stream()
            .filter(it -> it.getResult() != null && Integer.valueOf(1).equals(it.getResult().get("ID")))
            .findFirst().orElse(null);
        assertNotNull(item1, "Item for ID 1 not found or 'ID' not in result.");
        assertEquals("Test", item1.getResult().get("Name"));
        assertEquals(100, item1.getResult().get("Value"));
        // Check for a message indicating that the header was found after skipping lines, if applicable.
        // or that structure was detected with colIdxValueStart = 0
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Info [CSV]: Detected data structure from data row index 0: Hierarchy/Text columns: 0 to -1, Value/Numeric columns: 0 to 2.") || m.contains("First column ('ID') in first data row (data index 0) is numeric.")), "Expected message about structure detection for colIdxValueStart=0.");
    }
    
    @Test
    void testParseNoNumericCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("noNumeric");
        File file = getResourceFile("sample_csv_no_numeric.csv"); // ColA,ColB,ColC -> text1,text2,text3
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        // System.out.println("Messages (No Numeric): " + result.getParserLogMessages());
        // System.out.println("Items (No Numeric): " + result.getItems());
        
        // Refactored: Assumes last column "ColC" for values. text3 -> 0
        assertEquals(2, result.getItems().size()); 
        Item itemText1 = result.findItem("text1", result.getItems()).orElse(null);
        assertNotNull(itemText1);
        Item itemText1_text2 = result.findItem("text1text2", itemText1.getItems()).orElse(null);
        assertNotNull(itemText1_text2);
        assertEquals("text2", itemText1_text2.getName());
        assertEquals(0, itemText1_text2.getResult().get("ColC"));
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Warning [CSV]: No numeric columns auto-detected")), "Expected warning about no numeric columns.");
    }

    @Test
    void testParseOnlyValuesCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("onlyValues");
        File file = getResourceFile("sample_csv_only_values.csv"); // Val1,Val2,Val3 -> 10,20,30
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        // System.out.println("Messages (Only Values): " + result.getParserLogMessages());
        // System.out.println("Items (Only Values): " + result.getItems());
        // colIdxValueStart should be 0. All columns are values. Generic items per row.
        assertEquals(2, result.getItems().size()); 
        
        Item row1Item = result.getItems().get(0); 
        assertNotNull(row1Item.getResult());
        assertEquals(10, row1Item.getResult().get("Val1"));
        assertEquals(20, row1Item.getResult().get("Val2"));
        assertEquals(30, row1Item.getResult().get("Val3"));
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("Info [CSV]: First column ('Val1') is numeric. Treating it as the first value column.")), "Should log correct message for first column numeric. Messages: " + result.getParserLogMessages());
    }
    
    @Test
    void testParseMixedHierarchyValuesCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("mixed");
        File file = getResourceFile("sample_csv_mixed_hierarchy_values.csv");
        ReportDto result = parser.parse(file);
        assertNotNull(result);
        // System.out.println("Messages (Mixed Hier): " + result.getParserLogMessages());
        // System.out.println("Items (Mixed Hier): " + result.getItems().stream().map(Item::getId).collect(Collectors.joining(", ")));

        assertEquals(2, result.getItems().size(), "Expected Alpha and Beta as top-level items."); 

        Item alpha = result.findItem("Alpha", result.getItems()).orElse(null);
        assertNotNull(alpha, "Item 'Alpha' not found.");
        assertEquals(1, alpha.getItems().size(), "Alpha should have one sub-component: Auth");
        Item auth = result.findItem("AlphaAuth", alpha.getItems()).orElse(null);
        assertNotNull(auth, "Item 'AlphaAuth' not found.");
        assertEquals(2, auth.getItems().size(), "Auth should have two metrics: LoginTime, LogoutTime");
        
        Item loginTime = result.findItem("AlphaAuthLoginTime", auth.getItems()).orElse(null);
        assertNotNull(loginTime, "Item 'AlphaAuthLoginTime' not found.");
        assertEquals("LoginTime", loginTime.getName());
        assertEquals(120, loginTime.getResult().get("Value"));
        
        Item beta = result.findItem("Beta", result.getItems()).orElse(null);
        assertNotNull(beta, "Item 'Beta' not found.");
        Item db = result.findItem("BetaDB", beta.getItems()).orElse(null);
        assertNotNull(db, "Item 'BetaDB' not found.");
        Item queryTime = result.findItem("BetaDBQueryTime", db.getItems()).orElse(null);
        assertNotNull(queryTime, "Item 'BetaDBQueryTime' not found.");
        assertEquals(80, queryTime.getResult().get("Value"));
    }

    @Test
    void testParseOnlyHeaderCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("onlyHeader");
        // Assuming sample_csv_only_header.csv exists: ColA,ColB,ColC
        // This file might not have been created in the previous subtask if it was specific to Excel.
        // If it doesn't exist, this test will fail at getResourceFile.
        // For now, we assume it exists or will be created.
        // If not, we'd need to create it here:
        // Path tempFile = tempDir.resolve("sample_csv_only_header.csv");
        // Files.writeString(tempFile, "ColA,ColB,ColC");
        // File file = tempFile.toFile();
        File file = getResourceFile("sample_csv_only_header.csv"); 
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items when only header is present.");
        // System.out.println("Messages (Only Header CSV): " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("No data rows found after header.")), "Should log no data rows. Msgs: " + result.getParserLogMessages());
    }

    @Test
    void testParseEmptyCsv() throws IOException, URISyntaxException {
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("emptyCsv");
        // Assume sample_csv_empty.csv is an empty file.
        // Path tempFile = tempDir.resolve("sample_csv_empty.csv");
        // Files.writeString(tempFile, ""); // Create empty file
        // File file = tempFile.toFile();
        File file = getResourceFile("sample_csv_empty.csv"); 
        ReportDto result = parser.parse(file);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items for an empty CSV.");
        // System.out.println("Messages (Empty CSV): " + result.getParserLogMessages());
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.contains("No valid header row found")), "Should log no header or no content. Msgs: " + result.getParserLogMessages());
    }
    
    @Test
    void testParseNonCsvFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException { // Added @TempDir here
        Csv.CsvCustomParser parser = new Csv.CsvCustomParser("nonCsv");
        File nonCsvFile = Files.createFile(tempDir.resolve("test.txt")).toFile();
        Files.writeString(nonCsvFile.toPath(), "This is just a plain text file, not CSV.");
        
        ReportDto result = parser.parse(nonCsvFile);
        
        assertNotNull(result);
        assertTrue(result.getItems().isEmpty(), "Should have no items for a non-CSV file.");
        // System.out.println("Messages (Non-CSV): " + result.getParserLogMessages());
        // The parser might try to detect delimiter, fail or pick one, then fail to find header or data.
        // Or Jackson's CsvMapper might throw an early error.
        // The refactored code has a try-catch around MappingIterator creation.
        assertTrue(result.getParserLogMessages().stream().anyMatch(m -> m.toLowerCase().contains("error") || m.toLowerCase().contains("failed")), "Should log an error. Msgs: " + result.getParserLogMessages());
    }
}
