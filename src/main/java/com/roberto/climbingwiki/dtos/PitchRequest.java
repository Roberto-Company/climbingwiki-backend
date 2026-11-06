package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotNull;

public record PitchRequest(
        @NotNull Long routeId,
        Long gradeId,
        @NotNull Integer orderIndex,
        Integer lengthM,
        Integer boltsCount,
        String description) {}
