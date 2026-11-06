package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Crag;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CragResponse(
        Long id,
        Long zoneId,
        Long createdById,
        String name,
        String description,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer altitudeM,
        String orientation,
        String rockType,
        String approachDescription,
        LocalDateTime createdAt) {

    public static CragResponse from(Crag crag) {
        return new CragResponse(
                crag.getId(),
                crag.getZone() != null ? crag.getZone().getId() : null,
                crag.getCreatedBy() != null ? crag.getCreatedBy().getId() : null,
                crag.getName(),
                crag.getDescription(),
                crag.getLatitude(),
                crag.getLongitude(),
                crag.getAltitudeM(),
                crag.getOrientation(),
                crag.getRockType(),
                crag.getApproachDescription(),
                crag.getCreatedAt());
    }
}
