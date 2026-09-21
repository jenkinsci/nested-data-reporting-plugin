package io.jenkins.plugins.reporter.provider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility class to generate test Excel files for testing the Excel parser functionality.
 */
public class ExcelTestFileGenerator {

    public static void main(String[] args) throws IOException {
        // Create test files in the resources directory
        String resourcesDir = "src/test/resources";
        
        // Create normal Excel file
        createNormalExcelFile(new File(resourcesDir, "test-excel-normal.xlsx"));
        
        // Create Excel file with offset header
        createOffsetHeaderExcelFile(new File(resourcesDir, "test-excel-offset.xlsx"));
        
        // Create Excel file with mixed data
        createMixedDataExcelFile(new File(resourcesDir, "test-excel-mixed.xlsx"));
        
        System.out.println("Test Excel files created successfully in " + resourcesDir);
    }
    
    /**
     * Creates a normal Excel file with header in the first row and data below.
     */
    public static void createNormalExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Subcategory");
        headerRow.createCell(2).setCellValue("Value1");
        headerRow.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Category A");
        dataRow1.createCell(1).setCellValue("");
        dataRow1.createCell(2).setCellValue(10);
        dataRow1.createCell(3).setCellValue(20);
        
        Row dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("Category B");
        dataRow2.createCell(1).setCellValue("");
        dataRow2.createCell(2).setCellValue(30);
        dataRow2.createCell(3).setCellValue(40);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    /**
     * Creates an Excel file with header not in the first row.
     */
    public static void createOffsetHeaderExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        
        // Add some empty rows and cells
        sheet.createRow(0).createCell(0).setCellValue("This is not the header");
        sheet.createRow(1); // Empty row
        
        // Create header row at position 3
        Row headerRow = sheet.createRow(3);
        headerRow.createCell(0).setCellValue("Category");
        headerRow.createCell(1).setCellValue("Subcategory");
        headerRow.createCell(2).setCellValue("Value1");
        headerRow.createCell(3).setCellValue("Value2");
        
        // Create data rows
        Row dataRow1 = sheet.createRow(4);
        dataRow1.createCell(0).setCellValue("Category A");
        dataRow1.createCell(1).setCellValue("");
        dataRow1.createCell(2).setCellValue(10);
        dataRow1.createCell(3).setCellValue(20);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }

    /**
     * Creates an Excel file with mixed data types and nested structure.
     */
    public static void createMixedDataExcelFile(File file) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Test Sheet");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Parent");
        headerRow.createCell(1).setCellValue("Child");
        headerRow.createCell(2).setCellValue("Value1");
        headerRow.createCell(3).setCellValue("Value2");
        
        // Create data rows with nested structure
        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("Parent");
        dataRow1.createCell(1).setCellValue("Child");
        dataRow1.createCell(2).setCellValue(30);
        dataRow1.createCell(3).setCellValue(40);
        
        // Add some rows with different data types
        Row dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("Parent");
        dataRow2.createCell(1).setCellValue("Child2");
        dataRow2.createCell(2).setCellValue("Not a number"); // String instead of number
        dataRow2.createCell(3).setCellValue(50);
        
        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
