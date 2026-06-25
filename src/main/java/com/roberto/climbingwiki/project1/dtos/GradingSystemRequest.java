package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.GradingTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradingSystemRequest(@NotBlank String name, @NotNull GradingTypes.Category category) {}
