package io.jenkins.plugins.reporter;

import io.jenkins.plugins.reporter.model.Report;
import io.jenkins.plugins.reporter.provider.Json;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class ReportTest {
  
    @Test
    public void parseReport() throws IOException, IllegalAccessException, InstantiationException {
        File file1 = new File("/Users/simonsymhoven/Projects/nested-data-reporting-plugin/etc/report-1-part-1.json");
        File file2 = new File("/Users/simonsymhoven/Projects/nested-data-reporting-plugin/etc/report-1-part-2.json");
        File file3 = new File("/Users/simonsymhoven/Projects/nested-data-reporting-plugin/etc/report-1-part-3.json");
        
        Json.JsonParser parser = new Json.JsonParser();
        
        Report reportPart1 = parser.parse(file1).toReport();
        Report reportPart2 = parser.parse(file2).toReport();
        Report reportPart3 = parser.parse(file3).toReport();
        
        Report merged = reportPart1.merge(reportPart2).merge(reportPart3);
        
        System.out.println(merged);
    }
}
