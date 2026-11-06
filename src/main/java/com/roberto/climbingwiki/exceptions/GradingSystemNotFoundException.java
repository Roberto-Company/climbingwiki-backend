package com.roberto.climbingwiki.exceptions;

public class GradingSystemNotFoundException extends RuntimeException {

    public GradingSystemNotFoundException(Long id) {
        super("Grading system not found: id=" + id);
    }
}
