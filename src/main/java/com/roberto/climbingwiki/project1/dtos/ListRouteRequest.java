package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotNull;

public record ListRouteRequest(@NotNull Long listId, @NotNull Long routeId) {}
