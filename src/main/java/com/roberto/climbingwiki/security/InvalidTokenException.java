package com.roberto.climbingwiki.security;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException() {
        super("Invalid or expired token");
    }
}
