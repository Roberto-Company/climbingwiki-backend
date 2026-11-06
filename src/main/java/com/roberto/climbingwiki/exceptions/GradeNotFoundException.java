package com.roberto.climbingwiki.exceptions;

public class GradeNotFoundException extends RuntimeException {

    public GradeNotFoundException(Long id) {
        super("Grade not found: id=" + id);
    }
}
