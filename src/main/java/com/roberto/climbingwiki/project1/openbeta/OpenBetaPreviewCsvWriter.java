package com.roberto.climbingwiki.project1.openbeta;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Serializes {@link OpenBetaPreviewRow}s to CSV so import data can be reviewed before it touches the database. */
public final class OpenBetaPreviewCsvWriter {

    private static final String HEADER =
            "level,external_id,parent_external_id,name,latitude,longitude,route_type,"
                    + "grade_system,grade_label,grade_matched,length_m,first_ascent_by,first_ascent_date,issues";

    private OpenBetaPreviewCsvWriter() {}

    public static void write(List<OpenBetaPreviewRow> rows, Writer writer) throws IOException {
        writer.write(HEADER);
        writer.write("\n");
        for (OpenBetaPreviewRow row : rows) {
            writer.write(toCsvLine(row));
            writer.write("\n");
        }
    }

    private static String toCsvLine(OpenBetaPreviewRow row) {
        return String.join(
                ",",
                escape(row.level()),
                escape(row.externalId()),
                escape(row.parentExternalId()),
                escape(row.name()),
                escape(row.latitude()),
                escape(row.longitude()),
                escape(row.routeType()),
                escape(row.gradeSystem()),
                escape(row.gradeLabel()),
                escape(row.gradeMatched()),
                escape(row.lengthM()),
                escape(row.firstAscentBy()),
                escape(row.firstAscentDate()),
                escape(row.issues()));
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
