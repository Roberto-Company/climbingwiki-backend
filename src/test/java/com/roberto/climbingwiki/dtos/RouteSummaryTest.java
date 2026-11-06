package com.roberto.climbingwiki.dtos;

import static org.assertj.core.api.Assertions.assertThat;

import com.roberto.climbingwiki.repositories.PitchSummaryRow;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RouteSummaryTest {

    @Test
    void byRoute_picksHardestGradeAndSumsLengthsPerRoute() {
        Map<Long, RouteSummary> summaries = RouteSummary.byRoute(List.of(
                new PitchSummaryRow(1L, 10L, 50, 25),
                new PitchSummaryRow(1L, 12L, 70, 30),
                new PitchSummaryRow(1L, 11L, 60, null),
                new PitchSummaryRow(2L, 10L, 50, 18)));

        assertThat(summaries.get(1L)).isEqualTo(new RouteSummary(12L, 55));
        assertThat(summaries.get(2L)).isEqualTo(new RouteSummary(10L, 18));
    }

    @Test
    void byRoute_ungradedAndUnmeasuredPitches_giveNulls() {
        Map<Long, RouteSummary> summaries = RouteSummary.byRoute(List.of(new PitchSummaryRow(3L, null, null, null)));

        assertThat(summaries.get(3L)).isEqualTo(RouteSummary.EMPTY);
    }

    @Test
    void byRoute_noRows_isEmpty() {
        assertThat(RouteSummary.byRoute(List.of())).isEmpty();
    }
}
