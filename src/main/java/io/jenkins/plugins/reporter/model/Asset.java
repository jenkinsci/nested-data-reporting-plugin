package io.jenkins.plugins.reporter.model;

import net.sf.jsefa.csv.annotation.CsvDataType;
import net.sf.jsefa.csv.annotation.CsvField;

import java.io.Serializable;

@CsvDataType()
public class Asset implements Serializable {
    private static final long serialVersionUID = -2800979294230808946L;
    
    @CsvField(pos = 1)
    String id;

    @CsvField(pos = 2)
    int accurate;

    @CsvField(pos = 3)
    int manually;

    @CsvField(pos = 4)
    int incorrect;

    public String getId() {
        return id;
    }

    public int getAccurate() {
        return accurate;
    }

    public int getManually() {
        return manually;
    }

    public int getIncorrect() {
        return incorrect;
    }
}