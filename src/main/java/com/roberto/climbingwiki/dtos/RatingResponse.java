package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Rating;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RatingResponse(Long userId, Long routeId, BigDecimal score, LocalDateTime createdAt) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getUser().getId(), rating.getRoute().getId(), rating.getScore(), rating.getCreatedAt());
    }
}
