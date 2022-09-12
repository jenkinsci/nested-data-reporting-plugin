package io.jenkins.plugins.reporter.steps;

import com.google.errorprone.annotations.FormatMethod;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Report implements Serializable {

    private static final long serialVersionUID = 302445084497230108L;
    
    private List<String> infoMessages;
    
    private List<String> errorMessages;
    
    public Report() {
        this.infoMessages = new ArrayList<>();
        this.errorMessages = new ArrayList<>();
    }

    public List<String> getInfoMessages() {
        return this.infoMessages;
    }

    public List<String> getErrorMessages() {
        return this.errorMessages;
    }
    @FormatMethod
    public void logInfo(String format, Object... args) {
        this.infoMessages.add(String.format(format, args));
    }

    @FormatMethod
    public void logError(String format, Object... args) {
        this.errorMessages.add(String.format(format, args));
    }

    @FormatMethod
    public void logException(Exception exception, String format, Object... args) {
        this.logError(format, args);
        Collections.addAll(this.errorMessages, ExceptionUtils.getRootCauseStackTrace(exception));
    }
    
    
}
