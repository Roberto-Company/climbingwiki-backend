package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SectorRequest(
        @NotNull Long cragId,
        @NotBlank String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude) {}
