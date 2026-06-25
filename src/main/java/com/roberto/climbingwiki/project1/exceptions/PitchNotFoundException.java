package com.roberto.climbingwiki.project1.exceptions;

public class PitchNotFoundException extends RuntimeException {

    public PitchNotFoundException(Long id) {
        super("Pitch not found: id=" + id);
    }
}
