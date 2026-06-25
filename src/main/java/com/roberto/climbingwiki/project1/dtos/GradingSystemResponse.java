package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.GradingTypes;
import com.roberto.climbingwiki.project1.model.entities.GradingSystem;

public record GradingSystemResponse(Long id, String name, GradingTypes.Category category) {

    public static GradingSystemResponse from(GradingSystem gradingSystem) {
        return new GradingSystemResponse(gradingSystem.getId(), gradingSystem.getName(), gradingSystem.getCategory());
    }
}
