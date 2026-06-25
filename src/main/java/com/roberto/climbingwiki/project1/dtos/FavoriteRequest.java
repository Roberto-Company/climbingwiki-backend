package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotNull Long userId, @NotNull Long routeId) {}
