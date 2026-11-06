package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.GradingTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradingSystemRequest(@NotBlank String name, @NotNull GradingTypes.Category category) {}
