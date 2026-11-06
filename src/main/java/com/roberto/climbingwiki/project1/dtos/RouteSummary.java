package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.repositories.PitchSummaryRow;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A route's overall grade (its hardest pitch) and total length. Either value is null when no pitch
 * has a grade or a length.
 */
public record RouteSummary(Long hardestGradeId, Integer totalLengthM) {

    public static final RouteSummary EMPTY = new RouteSummary(null, null);

    /** Groups pitch rows by route. Routes without rows are absent; use {@link #EMPTY} for them. */
    public static Map<Long, RouteSummary> byRoute(List<PitchSummaryRow> rows) {
        Map<Long, Accumulator> byRoute = new HashMap<>();
        for (PitchSummaryRow row : rows) {
            byRoute.computeIfAbsent(row.routeId(), id -> new Accumulator()).add(row);
        }
        Map<Long, RouteSummary> result = new HashMap<>();
        byRoute.forEach((routeId, acc) -> result.put(routeId, acc.toSummary()));
        return Map.copyOf(result);
    }

    private static final class Accumulator {
        private Long hardestGradeId;
        private int hardestScore = Integer.MIN_VALUE;
        private Integer totalLength;

        void add(PitchSummaryRow row) {
            if (row.gradeId() != null && row.difficultyScore() != null && row.difficultyScore() > hardestScore) {
                hardestGradeId = row.gradeId();
                hardestScore = row.difficultyScore();
            }
            if (row.lengthM() != null) {
                totalLength = (totalLength == null ? 0 : totalLength) + row.lengthM();
            }
        }

        RouteSummary toSummary() {
            return new RouteSummary(hardestGradeId, totalLength);
        }
    }
}
