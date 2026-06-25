package com.roberto.climbingwiki.project1.exceptions;

public class InvalidFollowException extends RuntimeException {

    public InvalidFollowException() {
        super("A user cannot follow themselves");
    }
}
