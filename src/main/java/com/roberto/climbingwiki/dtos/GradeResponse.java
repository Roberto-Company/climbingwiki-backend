package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.Grade;

public record GradeResponse(Long id, Long gradingSystemId, String label, Integer difficultyScore) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(
                grade.getId(), grade.getGradingSystem().getId(), grade.getLabel(), grade.getDifficultyScore());
    }
}
