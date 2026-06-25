package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Favorite;
import java.time.LocalDateTime;

public record FavoriteResponse(Long userId, Long routeId, LocalDateTime createdAt) {

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getUser().getId(), favorite.getRoute().getId(), favorite.getCreatedAt());
    }
}
