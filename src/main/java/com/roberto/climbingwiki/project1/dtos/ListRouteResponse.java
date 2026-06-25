package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.ListRoute;
import java.time.LocalDateTime;

public record ListRouteResponse(Long listId, Long routeId, LocalDateTime addedAt) {

    public static ListRouteResponse from(ListRoute listRoute) {
        return new ListRouteResponse(
                listRoute.getList().getId(), listRoute.getRoute().getId(), listRoute.getAddedAt());
    }
}
