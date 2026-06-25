package com.roberto.climbingwiki.project1.openbeta;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenBetaPreviewCsvWriterTest {

    @Test
    void write_emitsHeaderAndRow() throws IOException {
        OpenBetaPreviewRow row = new OpenBetaPreviewRow(
                "ROUTE",
                "climb-1",
                "crag-1",
                "Vía X",
                36.9123,
                -4.7501,
                "SPORT",
                "French",
                "6a",
                true,
                25,
                "J. García",
                LocalDate.of(1998, 1, 1),
                "");
        StringWriter writer = new StringWriter();

        OpenBetaPreviewCsvWriter.write(List.of(row), writer);

        String[] lines = writer.toString().split("\n");
        assertThat(lines[0]).startsWith("level,external_id");
        assertThat(lines[1]).isEqualTo("ROUTE,climb-1,crag-1,Vía X,36.9123,-4.7501,SPORT,French,6a,true,25,J. García,1998-01-01,");
    }

    @Test
    void write_escapesCommasAndQuotesInFreeTextFields() throws IOException {
        OpenBetaPreviewRow row = new OpenBetaPreviewRow(
                "ROUTE",
                "climb-2",
                "crag-1",
                "Crack, The \"Big\" One",
                null,
                null,
                "TRAD",
                null,
                null,
                false,
                null,
                null,
                null,
                "MISSING_COORDINATES;UNMATCHED_GRADE");
        StringWriter writer = new StringWriter();

        OpenBetaPreviewCsvWriter.write(List.of(row), writer);

        String dataLine = writer.toString().split("\n")[1];
        assertThat(dataLine).contains("\"Crack, The \"\"Big\"\" One\"");
        assertThat(dataLine).contains("MISSING_COORDINATES;UNMATCHED_GRADE");
    }
}
