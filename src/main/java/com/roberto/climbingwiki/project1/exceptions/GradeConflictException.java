package com.roberto.climbingwiki.project1.exceptions;

public class GradeConflictException extends RuntimeException {

    public GradeConflictException(Long gradingSystemId, String label) {
        super("Grade with label=" + label + " already exists in grading system id=" + gradingSystemId);
    }
}
