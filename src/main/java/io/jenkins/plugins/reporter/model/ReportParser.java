package io.jenkins.plugins.reporter.model;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class ReportParser implements Serializable {
    private static final long serialVersionUID = -7720644051441434411L;
    public abstract ReportDto parse(File file) throws IOException;

}
