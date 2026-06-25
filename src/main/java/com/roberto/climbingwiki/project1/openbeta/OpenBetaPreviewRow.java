package com.roberto.climbingwiki.project1.openbeta;

import java.time.LocalDate;

/** One row of a human-reviewable preview of what an OpenBeta import would write, without touching the database. */
public record OpenBetaPreviewRow(
        String level,
        String externalId,
        String parentExternalId,
        String name,
        Double latitude,
        Double longitude,
        String routeType,
        String gradeSystem,
        String gradeLabel,
        boolean gradeMatched,
        Integer lengthM,
        String firstAscentBy,
        LocalDate firstAscentDate,
        String issues) {}
