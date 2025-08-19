package io.jenkins.plugins.reporter.provider;

import io.jenkins.plugins.reporter.model.ReportDto;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExcelTest {

    @Test
    public void shouldParseExcelFile() throws IOException {
        Excel.ExcelParser parser = new Excel.ExcelParser("excel");
        File file = new File("src/test/resources/test.xlsx");
        ReportDto report = parser.parse(file);

        System.out.println(report.getItems());

        assertThat(report.getItems()).hasSize(2);
        assertThat(report.getItems().get(0).getName()).isEqualTo("1.0");
        assertThat(report.getItems().get(0).getResult().get("Value")).isEqualTo(10);
        assertThat(report.getItems().get(1).getName()).isEqualTo("2.0");
        assertThat(report.getItems().get(1).getResult().get("Value")).isEqualTo(20);
    }
}
