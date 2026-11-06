package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteListRequest(
        @NotNull Long userId, @NotBlank String name, String description, Boolean isPublic) {}
