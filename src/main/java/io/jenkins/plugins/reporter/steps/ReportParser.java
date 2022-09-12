package io.jenkins.plugins.reporter.steps;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public abstract class ReportParser implements Serializable {
    private static final long serialVersionUID = -7720644051441434411L;
    public abstract Report parse(File file) throws IOException;

}
