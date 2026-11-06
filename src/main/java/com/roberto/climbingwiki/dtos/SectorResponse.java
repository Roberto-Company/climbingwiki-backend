package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Sector;
import java.math.BigDecimal;
import java.util.Set;

public record SectorResponse(
        Long id,
        Long cragId,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Set<Route.RouteType> routeTypes,
        Long createdById,
        boolean verified) {

    public static SectorResponse from(Sector sector, Set<Route.RouteType> routeTypes) {
        return new SectorResponse(
                sector.getId(),
                sector.getCrag().getId(),
                sector.getName(),
                sector.getDescription(),
                sector.getLatitude(),
                sector.getLongitude(),
                routeTypes,
                sector.getCreatedById(),
                sector.isVerified());
    }
}
