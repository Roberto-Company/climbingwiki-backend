package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.RouteList;
import java.time.LocalDateTime;

public record RouteListResponse(
        Long id, Long userId, String name, String description, boolean isPublic, LocalDateTime createdAt) {

    public static RouteListResponse from(RouteList routeList) {
        return new RouteListResponse(
                routeList.getId(),
                routeList.getUser().getId(),
                routeList.getName(),
                routeList.getDescription(),
                routeList.isPublic(),
                routeList.getCreatedAt());
    }
}
