package com.roberto.climbingwiki.exceptions;

public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException() {
        super("Too many login attempts, try again later");
    }
}
