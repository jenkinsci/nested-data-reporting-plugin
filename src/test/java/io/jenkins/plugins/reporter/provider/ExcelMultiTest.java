package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.ReportDto;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExcelMultiTest {

    @Test
    public void shouldParseExcelFile() throws IOException {
        ExcelMulti.ExcelMultiParser parser = new ExcelMulti.ExcelMultiParser("excelMulti");
        File file = new File("src/test/resources/test_multi.xlsx");
        ReportDto report = parser.parse(file);
        assertThat(report.getItems()).hasSize(4);
        assertThat(report.getItems().get(0).getName()).isEqualTo("1.0");
        assertThat(report.getItems().get(0).getResult().get("Value")).isEqualTo(10);
        assertThat(report.getItems().get(1).getName()).isEqualTo("2.0");
        assertThat(report.getItems().get(1).getResult().get("Value")).isEqualTo(20);
        assertThat(report.getItems().get(2).getName()).isEqualTo("3.0");
        assertThat(report.getItems().get(2).getResult().get("Value")).isEqualTo(30);
        assertThat(report.getItems().get(3).getName()).isEqualTo("4.0");
        assertThat(report.getItems().get(3).getResult().get("Value")).isEqualTo(40);
    }

    @Test(expected = IOException.class)
    public void shouldThrowExceptionForInconsistentHeaders() throws IOException {
        ExcelMulti.ExcelMultiParser parser = new ExcelMulti.ExcelMultiParser("excelMulti");
        File file = new File("src/test/resources/test_multi_inconsistent.xlsx");
        parser.parse(file);
    }
}
