package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Route;
import com.roberto.climbingwiki.model.entities.Zone;
import java.util.Set;

public record ZoneResponse(
        Long id, Long parentZoneId, String name, String description, Set<Route.RouteType> routeTypes) {

    public static ZoneResponse from(Zone zone, Set<Route.RouteType> routeTypes) {
        return new ZoneResponse(
                zone.getId(),
                zone.getParentZone() != null ? zone.getParentZone().getId() : null,
                zone.getName(),
                zone.getDescription(),
                routeTypes);
    }
}
