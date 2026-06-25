package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Tag;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

public record RouteResponse(
        Long id,
        Long sectorId,
        Long createdById,
        String name,
        Route.RouteType routeType,
        String description,
        String equipmentNotes,
        String firstAscentBy,
        LocalDate firstAscentDate,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Set<String> tags) {

    public static RouteResponse from(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getSector().getId(),
                route.getCreatedBy() != null ? route.getCreatedBy().getId() : null,
                route.getName(),
                route.getRouteType(),
                route.getDescription(),
                route.getEquipmentNotes(),
                route.getFirstAscentBy(),
                route.getFirstAscentDate(),
                route.getLatitude(),
                route.getLongitude(),
                route.getCreatedAt(),
                route.getUpdatedAt(),
                route.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
    }
}
