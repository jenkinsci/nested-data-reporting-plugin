package io.jenkins.plugins.reporter.provider;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Utilitaire pour créer un fichier Excel de test dans le répertoire etc/
 */
public class CreateExcelSample {
    
    public static void main(String[] args) {
        String filePath = "/home/ubuntu/workspace/nested-data-reporting-plugin/etc/report.xlsx";
        
        try {
            // Créer un nouveau classeur Excel
            Workbook workbook = new XSSFWorkbook();
            
            // Créer une feuille
            Sheet sheet = workbook.createSheet("Sample Data");
            
            // Créer l'en-tête
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Category");
            headerRow.createCell(1).setCellValue("Subcategory");
            headerRow.createCell(2).setCellValue("Value1");
            headerRow.createCell(3).setCellValue("Value2");
            
            // Créer les données
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Category A");
            row1.createCell(1).setCellValue("");
            row1.createCell(2).setCellValue(10);
            row1.createCell(3).setCellValue(20);
            
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Category B");
            row2.createCell(1).setCellValue("");
            row2.createCell(2).setCellValue(30);
            row2.createCell(3).setCellValue(40);
            
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Category C");
            row3.createCell(1).setCellValue("");
            row3.createCell(2).setCellValue(50);
            row3.createCell(3).setCellValue(60);
            
            // Écrire dans le fichier
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }
            
            // Fermer le classeur
            workbook.close();
            
            System.out.println("Fichier Excel créé avec succès à " + filePath);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
