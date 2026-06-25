package com.roberto.climbingwiki.project1.exceptions;

public class GradingSystemConflictException extends RuntimeException {

    public GradingSystemConflictException(String name) {
        super("Grading system with name=" + name + " already exists");
    }
}
