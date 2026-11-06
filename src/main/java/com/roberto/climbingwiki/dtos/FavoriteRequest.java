package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotNull;

public record FavoriteRequest(@NotNull Long userId, @NotNull Long routeId) {}
