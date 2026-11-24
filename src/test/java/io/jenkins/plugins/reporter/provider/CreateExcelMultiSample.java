package io.jenkins.plugins.reporter.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class to generate multi-tab Excel test files for testing the ExcelMulti provider functionality.
 */
public class CreateExcelMultiSample {

    public static void main(String[] args) throws IOException {
        // Create test files in the resources directory
        String resourcesDir = "src/test/resources";
        
        // Create multi-sheet Excel file with consistent headers
        createConsistentHeadersExcelFile(new File(resourcesDir, "test-excel-multi-consistent.xlsx"));
        
        // Create multi-sheet Excel file with inconsistent headers
        createInconsistentHeadersExcelFile(new File(resourcesDir, "test-excel-multi-inconsistent.xlsx"));
        
        // Create multi-sheet Excel file with mixed validity
        createMixedValidityExcelFile(new File(resourcesDir, "test-excel-multi-mixed.xlsx"));
        
        System.out.println("Multi-tab test Excel files created successfully in " + resourcesDir);
    }
    
    /**
     * Creates a multi-sheet Excel file with consistent headers across all sheets.
     */
    public static void createConsistentHeadersExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Create first sheet
        Sheet sheet1 = workbook.createSheet("Sheet 1");
        
        // Create header row
        Row headerRow1 = sheet1.createRow(0);
        headerRow1.createCell(0).setCellValue("Category");
        headerRow1.createCell(1).setCellValue("Subcategory");
        headerRow1.createCell(2).setCellValue("Value1");
        headerRow1.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow1 = sheet1.createRow(1);
        dataRow1.createCell(0).setCellValue("Category A");
        dataRow1.createCell(1).setCellValue("Subcat A1");
        dataRow1.createCell(2).setCellValue(10);
        dataRow1.createCell(3).setCellValue(20);
        
        Row dataRow2 = sheet1.createRow(2);
        dataRow2.createCell(0).setCellValue("Category A");
        dataRow2.createCell(1).setCellValue("Subcat A2");
        dataRow2.createCell(2).setCellValue(15);
        dataRow2.createCell(3).setCellValue(25);
        
        // Create second sheet with the same headers
        Sheet sheet2 = workbook.createSheet("Sheet 2");
        
        // Create identical header row
        Row headerRow2 = sheet2.createRow(0);
        headerRow2.createCell(0).setCellValue("Category");
        headerRow2.createCell(1).setCellValue("Subcategory");
        headerRow2.createCell(2).setCellValue("Value1");
        headerRow2.createCell(3).setCellValue("Value2");
        
        // Create data rows with different data
        Row dataRow3 = sheet2.createRow(1);
        dataRow3.createCell(0).setCellValue("Category B");
        dataRow3.createCell(1).setCellValue("Subcat B1");
        dataRow3.createCell(2).setCellValue(30);
        dataRow3.createCell(3).setCellValue(40);
        
        Row dataRow4 = sheet2.createRow(2);
        dataRow4.createCell(0).setCellValue("Category B");
        dataRow4.createCell(1).setCellValue("Subcat B2");
        dataRow4.createCell(2).setCellValue(35);
        dataRow4.createCell(3).setCellValue(45);
        
        // Create third sheet with the same headers
        Sheet sheet3 = workbook.createSheet("Sheet 3");
        
        // Create identical header row
        Row headerRow3 = sheet3.createRow(0);
        headerRow3.createCell(0).setCellValue("Category");
        headerRow3.createCell(1).setCellValue("Subcategory");
        headerRow3.createCell(2).setCellValue("Value1");
        headerRow3.createCell(3).setCellValue("Value2");
        
        // Create data rows with different data
        Row dataRow5 = sheet3.createRow(1);
        dataRow5.createCell(0).setCellValue("Category C");
        dataRow5.createCell(1).setCellValue("Subcat C1");
        dataRow5.createCell(2).setCellValue(50);
        dataRow5.createCell(3).setCellValue(60);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
    
    /**
     * Creates a multi-sheet Excel file with inconsistent headers across sheets.
     */
    public static void createInconsistentHeadersExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Create first sheet
        Sheet sheet1 = workbook.createSheet("Sheet 1");
        
        // Create header row
        Row headerRow1 = sheet1.createRow(0);
        headerRow1.createCell(0).setCellValue("Category");
        headerRow1.createCell(1).setCellValue("Subcategory");
        headerRow1.createCell(2).setCellValue("Value1");
        headerRow1.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow1 = sheet1.createRow(1);
        dataRow1.createCell(0).setCellValue("Category A");
        dataRow1.createCell(1).setCellValue("Subcat A1");
        dataRow1.createCell(2).setCellValue(10);
        dataRow1.createCell(3).setCellValue(20);
        
        // Create second sheet with different headers
        Sheet sheet2 = workbook.createSheet("Sheet 2");
        
        // Create different header row (different column order)
        Row headerRow2 = sheet2.createRow(0);
        headerRow2.createCell(0).setCellValue("Category");
        headerRow2.createCell(1).setCellValue("Value1");  // Swapped order
        headerRow2.createCell(2).setCellValue("Subcategory");
        headerRow2.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow2 = sheet2.createRow(1);
        dataRow2.createCell(0).setCellValue("Category B");
        dataRow2.createCell(1).setCellValue(30);
        dataRow2.createCell(2).setCellValue("Subcat B1");
        dataRow2.createCell(3).setCellValue(40);
        
        // Create third sheet with different headers
        Sheet sheet3 = workbook.createSheet("Sheet 3");
        
        // Create different header row (different column names)
        Row headerRow3 = sheet3.createRow(0);
        headerRow3.createCell(0).setCellValue("Group");  // Different name
        headerRow3.createCell(1).setCellValue("Subcategory");
        headerRow3.createCell(2).setCellValue("Score1");  // Different name
        headerRow3.createCell(3).setCellValue("Score2");  // Different name
        
        // Create data rows
        Row dataRow3 = sheet3.createRow(1);
        dataRow3.createCell(0).setCellValue("Category C");
        dataRow3.createCell(1).setCellValue("Subcat C1");
        dataRow3.createCell(2).setCellValue(50);
        dataRow3.createCell(3).setCellValue(60);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
    
    /**
     * Creates a multi-sheet Excel file with mixed validity (some valid sheets, some invalid).
     */
    public static void createMixedValidityExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        
        // Create first sheet (valid)
        Sheet sheet1 = workbook.createSheet("Valid Sheet 1");
        
        // Create header row
        Row headerRow1 = sheet1.createRow(0);
        headerRow1.createCell(0).setCellValue("Category");
        headerRow1.createCell(1).setCellValue("Subcategory");
        headerRow1.createCell(2).setCellValue("Value1");
        headerRow1.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow1 = sheet1.createRow(1);
        dataRow1.createCell(0).setCellValue("Category A");
        dataRow1.createCell(1).setCellValue("Subcat A1");
        dataRow1.createCell(2).setCellValue(10);
        dataRow1.createCell(3).setCellValue(20);
        
        // Create second sheet (empty - invalid)
        workbook.createSheet("Empty Sheet");
        
        // Create third sheet (valid - same header)
        Sheet sheet3 = workbook.createSheet("Valid Sheet 2");
        
        // Create header row (same as first sheet)
        Row headerRow3 = sheet3.createRow(0);
        headerRow3.createCell(0).setCellValue("Category");
        headerRow3.createCell(1).setCellValue("Subcategory");
        headerRow3.createCell(2).setCellValue("Value1");
        headerRow3.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow3 = sheet3.createRow(1);
        dataRow3.createCell(0).setCellValue("Category C");
        dataRow3.createCell(1).setCellValue("Subcat C1");
        dataRow3.createCell(2).setCellValue(50);
        dataRow3.createCell(3).setCellValue(60);
        
        // Create fourth sheet (invalid - fewer columns)
        Sheet sheet4 = workbook.createSheet("Invalid Sheet - Fewer Columns");
        
        // Create header row with fewer columns
        Row headerRow4 = sheet4.createRow(0);
        headerRow4.createCell(0).setCellValue("Category");
        headerRow4.createCell(1).setCellValue("Value1");
        
        // Create data row
        Row dataRow4 = sheet4.createRow(1);
        dataRow4.createCell(0).setCellValue("Category D");
        dataRow4.createCell(1).setCellValue(70);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}

