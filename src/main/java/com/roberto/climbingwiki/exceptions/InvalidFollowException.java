package com.roberto.climbingwiki.exceptions;

public class InvalidFollowException extends RuntimeException {

    public InvalidFollowException() {
        super("A user cannot follow themselves");
    }
}
