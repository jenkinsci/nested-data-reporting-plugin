package io.jenkins.plugins.reporter.provider;

import java.io.File;
import java.io.IOException;

/**
 * Temporary utility class to regenerate test Excel files.
 * Run this class to update test files before running tests.
 */
public class RegenerateTestFiles {
    
    public static void main(String[] args) throws IOException {
        // Temporary file to regenerate test data
        File resourcesDir = new File("src/test/resources");
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }

        System.out.println("Regenerating test Excel files...");
        
        CreateExcelMultiSample.createConsistentHeadersExcelFile(
            new File(resourcesDir, "test-excel-multi-consistent.xlsx"));
        CreateExcelMultiSample.createInconsistentHeadersExcelFile(
            new File(resourcesDir, "test-excel-multi-inconsistent.xlsx"));
        CreateExcelMultiSample.createMixedValidityExcelFile(
            new File(resourcesDir, "test-excel-multi-mixed.xlsx"));
            
        System.out.println("Test Excel files regenerated successfully!");
    }
}

