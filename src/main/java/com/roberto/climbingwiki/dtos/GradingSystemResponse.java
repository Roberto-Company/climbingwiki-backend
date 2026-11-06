package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.GradingTypes;
import com.roberto.climbingwiki.model.entities.GradingSystem;

public record GradingSystemResponse(Long id, String name, GradingTypes.Category category) {

    public static GradingSystemResponse from(GradingSystem gradingSystem) {
        return new GradingSystemResponse(gradingSystem.getId(), gradingSystem.getName(), gradingSystem.getCategory());
    }
}
