package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Route;
import com.roberto.climbingwiki.project1.model.entities.Sector;
import java.math.BigDecimal;
import java.util.Set;

public record SectorResponse(
        Long id,
        Long cragId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Set<Route.RouteType> routeTypes) {

    public static SectorResponse from(Sector sector, Set<Route.RouteType> routeTypes) {
        return new SectorResponse(
                sector.getId(),
                sector.getCrag().getId(),
                sector.getName(),
                sector.getDescription(),
                sector.getLatitude(),
                sector.getLongitude(),
                routeTypes);
    }
}
