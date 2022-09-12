package io.jenkins.plugins.reporter.steps;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;

public class FilesScanner extends MasterToSlaveFileCallable<Report>  {

    private static final long serialVersionUID = 4472630373073191961L;
    
    private final String filePattern;

    /**
     * Creates a new instance of {@link io.jenkins.plugins.reporter.model.FilesScanner}.
     *
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     */
    public FilesScanner(final String filePattern) {
        super();

        this.filePattern = filePattern;
    }

    @Override
    public Report invoke(File workspace, VirtualChannel channel) throws IOException, InterruptedException {
        Report report = new Report();
        report.logInfo("Searching for all files in '%s' that match the pattern '%s'",
                workspace.getAbsolutePath(), filePattern);
        
        return report;
    }
}
