package com.roberto.climbingwiki.exceptions;

public class PitchNotFoundException extends RuntimeException {

    public PitchNotFoundException(Long id) {
        super("Pitch not found: id=" + id);
    }
}
