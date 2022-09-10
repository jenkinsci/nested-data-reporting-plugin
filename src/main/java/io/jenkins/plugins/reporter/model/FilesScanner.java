package io.jenkins.plugins.reporter.model;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FilesScanner extends MasterToSlaveFileCallable<List<Result>> {

    private final String filePattern;

    /**
     * Creates a new instance of {@link FilesScanner}.
     *
     * @param filePattern
     *         ant file-set pattern to scan for files to parse
     */
    public FilesScanner(final String filePattern) {
        super();

        this.filePattern = filePattern;
    }
    
    @Override
    public List<Result> invoke(final File workspace, final VirtualChannel channel) throws IOException, InterruptedException {
        List<Result> results = new ArrayList<>();
        
        String[] fileNames = new FileFinder(filePattern).find(workspace);

        if (fileNames.length > 0) {
            scanFiles(workspace, fileNames, results);
        }
        
        return results;
    }

    private void scanFiles(final File workspace, final String[] fileNames, final List<Result> results) throws IOException {
        ResultParser parser = new ResultParser();
        for (String fileName : fileNames) {
            Path file = workspace.toPath().resolve(fileName);

            if (!Files.isReadable(file)) {
                System.out.printf("Skipping file '%s' because Jenkins has no permission to read the file", fileName);
            }
            else if (isEmpty(file)) {
                System.out.printf("Skipping file '%s' because it's empty", fileName);
            }
            else {
                Optional<Result> result = parser.parseResult(file.toFile());
                result.ifPresent(results::add);
            }
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
}
