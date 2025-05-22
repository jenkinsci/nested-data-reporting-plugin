package io.jenkins.plugins.reporter.model;

import java.io.Serializable;

public class ExcelParserConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // Future configuration options can be added here, for example:
    // private int headerRowIndex = 0; // Default to the first row
    // private int dataStartRowIndex = 1; // Default to the second row
    // private String sheetName; // For single sheet parsing, if specified
    // private boolean detectHeadersAutomatically = true;

    public ExcelParserConfig() {
        // Default constructor
    }

    // Add getters and setters here if fields are added in the future.
}
