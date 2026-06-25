package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CragRequest(
        Long zoneId,
        Long createdById,
        @NotBlank String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer altitudeM,
        String orientation,
        String rockType,
        String approachDescription) {}
