package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.Photo;
import java.time.LocalDateTime;

public record PhotoResponse(
        Long id,
        Long userId,
        Long routeId,
        Long cragId,
        String url,
        String caption,
        boolean isTopo,
        LocalDateTime createdAt) {

    public static PhotoResponse from(Photo photo) {
        return new PhotoResponse(
                photo.getId(),
                photo.getUser().getId(),
                photo.getRoute() != null ? photo.getRoute().getId() : null,
                photo.getCrag() != null ? photo.getCrag().getId() : null,
                photo.getUrl(),
                photo.getCaption(),
                photo.isTopo(),
                photo.getCreatedAt());
    }
}
