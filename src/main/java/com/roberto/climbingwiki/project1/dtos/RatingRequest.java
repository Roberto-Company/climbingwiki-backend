package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RatingRequest(
        @NotNull Long userId,
        @NotNull Long routeId,
        @NotNull @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal score) {}
