package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PhotoRequest(
        @NotNull Long userId,
        Long routeId,
        Long cragId,
        @NotBlank String url,
        String caption,
        Boolean isTopo) {}
