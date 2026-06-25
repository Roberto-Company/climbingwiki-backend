package com.roberto.climbingwiki.project1.exceptions;

public class GradeNotFoundException extends RuntimeException {

    public GradeNotFoundException(Long id) {
        super("Grade not found: id=" + id);
    }
}
