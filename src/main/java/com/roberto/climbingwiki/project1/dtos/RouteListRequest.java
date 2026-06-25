package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RouteListRequest(
        @NotNull Long userId, @NotBlank String name, String description, Boolean isPublic) {}
