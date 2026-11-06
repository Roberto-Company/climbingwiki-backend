package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Pitch;

public record PitchResponse(
        Long id,
        Long routeId,
        Long gradeId,
        Integer orderIndex,
        Integer lengthM,
        Integer boltsCount,
        String description,
        Long createdById,
        boolean verified) {

    public static PitchResponse from(Pitch pitch) {
        return new PitchResponse(
                pitch.getId(),
                pitch.getRoute().getId(),
                pitch.getGrade() != null ? pitch.getGrade().getId() : null,
                pitch.getOrderIndex(),
                pitch.getLengthM(),
                pitch.getBoltsCount(),
                pitch.getDescription(),
                pitch.getCreatedById(),
                pitch.isVerified());
    }
}
