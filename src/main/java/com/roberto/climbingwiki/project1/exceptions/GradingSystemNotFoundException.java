package com.roberto.climbingwiki.project1.exceptions;

public class GradingSystemNotFoundException extends RuntimeException {

    public GradingSystemNotFoundException(Long id) {
        super("Grading system not found: id=" + id);
    }
}
