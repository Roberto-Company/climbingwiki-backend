package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Favorite;
import java.time.LocalDateTime;

public record FavoriteResponse(Long userId, Long routeId, LocalDateTime createdAt) {

    public static FavoriteResponse from(Favorite favorite) {
        return new FavoriteResponse(
                favorite.getUser().getId(), favorite.getRoute().getId(), favorite.getCreatedAt());
    }
}
