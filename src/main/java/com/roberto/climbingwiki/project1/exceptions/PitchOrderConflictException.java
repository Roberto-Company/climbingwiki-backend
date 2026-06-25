package com.roberto.climbingwiki.project1.exceptions;

public class PitchOrderConflictException extends RuntimeException {

    public PitchOrderConflictException(Long routeId, Integer orderIndex) {
        super("Pitch orderIndex=" + orderIndex + " already used on route id=" + routeId);
    }
}
