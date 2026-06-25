package com.roberto.climbingwiki.project1.exceptions;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        super("You do not have permission to perform this action");
    }
}
