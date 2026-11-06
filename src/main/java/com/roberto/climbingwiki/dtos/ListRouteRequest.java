package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotNull;

public record ListRouteRequest(@NotNull Long listId, @NotNull Long routeId) {}
