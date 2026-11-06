package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Route;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record RouteRequest(
        @NotNull Long sectorId,
        Long createdById,
        @NotBlank String name,
        @NotNull Route.RouteType routeType,
        String description,
        String equipmentNotes,
        String firstAscentBy,
        LocalDate firstAscentDate,
        BigDecimal latitude,
        BigDecimal longitude,
        Set<Long> tagIds) {}
