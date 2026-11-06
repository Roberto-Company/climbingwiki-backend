package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradeRequest(
        @NotNull Long gradingSystemId, @NotBlank String label, @NotNull Integer difficultyScore) {}
