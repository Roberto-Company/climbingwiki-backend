package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Tag;
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
        Set<String> tags,
        Long hardestGradeId,
        Integer totalLengthM,
        boolean verified) {

    public static RouteResponse from(Route route, RouteSummary summary) {
        return new RouteResponse(
                route.getId(),
                route.getSector().getId(),
                route.getCreatedById(),
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
                route.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                summary.hardestGradeId(),
                summary.totalLengthM(),
                route.isVerified());
    }
}
