package com.roberto.climbingwiki.exceptions;

public class InvalidOrExpiredTokenException extends RuntimeException {

    public InvalidOrExpiredTokenException() {
        super("Invalid or expired token");
    }
}
