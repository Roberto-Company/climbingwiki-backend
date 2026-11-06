package com.roberto.climbingwiki.exceptions;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
