package io.jenkins.plugins.reporter.util;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.selectors.TypeSelector;

import java.io.File;
import java.io.IOException;

public class FileFinder extends MasterToSlaveFileCallable<String[]> {

    private final String includesPattern;


    /**
     * Creates a new instance of {@link FileFinder}.
     *
     * @param includesPattern
     *         the ant file includes pattern to scan for
     */
    public FileFinder(final String includesPattern) {
        super();

        this.includesPattern = includesPattern;
    }

    /**
     * Returns an array with the file names of the specified file pattern that have been found in the workspace.
     *
     * @param workspace
     *         root directory of the workspace
     * @param channel
     *         not used
     *
     * @return the file names of all found files
     * @throws IOException
     *         if the workspace could not be read
     */
    @Override
    public String[] invoke(final File workspace, final VirtualChannel channel) throws IOException, InterruptedException {
        return find(workspace);
    }

    /**
     * Returns an array with the file names of the specified file pattern that have been found in the workspace.
     *
     * @param workspace
     *         root directory of the workspace
     *
     * @return the file names of all found files
     */
    public String[] find(final File workspace) {
        try {
            FileSet fileSet = new FileSet();
            Project antProject = new Project();
            fileSet.setProject(antProject);
            fileSet.setDir(workspace);
            fileSet.setIncludes(includesPattern);
            TypeSelector selector = new TypeSelector();
            TypeSelector.FileType fileType = new TypeSelector.FileType();
            fileType.setValue(TypeSelector.FileType.FILE);
            selector.setType(fileType);
            fileSet.addType(selector);

            return fileSet.getDirectoryScanner(antProject).getIncludedFiles();
        }
        catch (BuildException ignored) {
            return new String[0]; // as fallback do not return any file
        }
    }
}
