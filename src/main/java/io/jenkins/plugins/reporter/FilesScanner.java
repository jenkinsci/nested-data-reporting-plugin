package io.jenkins.plugins.reporter;

import hudson.remoting.VirtualChannel;
import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.model.ReportParser;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesScanner extends MasterToSlaveFileCallable<Report>  {

    private static final long serialVersionUID = 4472630373073191961L;
    
    private final String filePattern;

    private final ReportParser parser;
    
    /**
     * Creates a new instance of {@link FilesScanner}.
     *
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     */
    public FilesScanner(final String filePattern, ReportParser parser) {
        super();
        this.filePattern = filePattern;
        this.parser = parser;
    }

    @Override
    public Report invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        
        Report report = new Report();
        report.logInfo("Searching for all files in '%s' that match the pattern '%s'",
                workspace.getAbsolutePath(), filePattern);

        String[] fileNames = new FileFinder(filePattern).find(workspace);

        if (fileNames.length == 0) {
            report.logError("No files found for pattern '%s'. Configuration error?", filePattern);
        }
        else {
            report.logInfo("-> found %s", plural(fileNames.length));
            scanFiles(workspace, fileNames, report);
        }
        
        return report;
    }
    
    private void scanFiles(final File workspace, final String[] fileNames, final Report report) throws IOException {
        for (String fileName : fileNames) {
            Path file = workspace.toPath().resolve(fileName);

            if (!Files.isReadable(file)) {
                report.logError("Skipping file '%s' because Jenkins has no permission to read the file", fileName);
            }
            else if (isEmpty(file)) {
                report.logError("Skipping file '%s' because it's empty", fileName);
            }
            else {
                aggregateReport(file, report);
            }
        }
    }

    private void aggregateReport(final Path file, final Report aggregatedReport) {
        try {
            Report report = parser.parse(file.toFile()).toReport();;
            aggregatedReport.logInfo("Successfully parsed file %s", file);
            aggregatedReport.add(report);
        } catch (IOException exception) {
            aggregatedReport.logException(exception, "Parsing of file '%s' failed due to an exception:", file);
        }
    }

    private boolean isEmpty(final Path file) {
        try {
            return Files.size(file) <= 0;
        }
        catch (IOException e) {
            return true;
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private String plural(final int count) {
        StringBuilder builder = new StringBuilder("file");
        if (count != 1) {
            builder.append('s');
        }
        builder.insert(0, ' ');
        builder.insert(0, count);
        return builder.toString();
    }
}
