package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Rating;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RatingResponse(Long userId, Long routeId, BigDecimal score, LocalDateTime createdAt) {

    public static RatingResponse from(Rating rating) {
        return new RatingResponse(
                rating.getUser().getId(), rating.getRoute().getId(), rating.getScore(), rating.getCreatedAt());
    }
}
