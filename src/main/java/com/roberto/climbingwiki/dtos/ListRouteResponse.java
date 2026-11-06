package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.ListRoute;
import java.time.LocalDateTime;

public record ListRouteResponse(Long listId, Long routeId, LocalDateTime addedAt) {

    public static ListRouteResponse from(ListRoute listRoute) {
        return new ListRouteResponse(
                listRoute.getList().getId(), listRoute.getRoute().getId(), listRoute.getAddedAt());
    }
}
